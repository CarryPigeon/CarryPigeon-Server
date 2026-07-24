package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.CreateTokenSessionCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenPair;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthSessionTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 鉴权会话领域 API 实现。
 * 职责：直接承载登录、验证码会话、刷新和注销用例实现。
 * 边界：不负责账号注册和验证码发送入口。
 */
@Service
public class AuthSessionDomainApi implements AuthSessionApi {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final AuthAccountProvisioner authAccountProvisioner;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AuthTokenCodec authTokenCodec;
    private final AuthTokenIssuer authTokenIssuer;
    private final AuthTokenSettings authTokenSettings;
    private final AuthPasswordLoginPolicy passwordLoginPolicy;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationApi emailVerificationApi;

    @Autowired
    public AuthSessionDomainApi(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelAccountProvisioningApi channelAccountProvisioningApi,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenCodec authTokenCodec,
            AuthTokenSettings authTokenSettings,
            AuthPasswordLoginPolicy passwordLoginPolicy,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.authAccountProvisioner = new AuthAccountProvisioner(
                userAccountProvisioningApi,
                channelAccountProvisioningApi
        );
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.authTokenCodec = authTokenCodec;
        this.authTokenIssuer = new AuthTokenIssuer(
                authRefreshSessionRepository,
                tokenHasher,
                authTokenCodec,
                authTokenSettings,
                idGenerator,
                timeProvider
        );
        this.authTokenSettings = authTokenSettings;
        this.passwordLoginPolicy = passwordLoginPolicy;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationApi = emailVerificationApi;
    }

    @Override
    public AuthSessionTokenResult createTokenSession(CreateTokenSessionCommand command) {
        if (!"email_code".equals(command.grantType())) {
            throw ProblemException.validationFailed("grant_type must be email_code");
        }
        String normalizedEmail = normalizeEmail(command.email());
        emailVerificationApi.verifyCode(new VerifyEmailVerificationCodeCommand(normalizedEmail, command.code()));
        return transactionRunner.runInTransaction(() -> {
            AuthAccount existingAccount = authAccountRepository.findByUsername(normalizedEmail).orElse(null);
            boolean newUser = existingAccount == null;
            AuthAccount account = existingAccount;
            if (account == null) {
                Instant now = timeProvider.nowInstant();
                account = authAccountRepository.save(new AuthAccount(
                        idGenerator.nextLongId(),
                        normalizedEmail,
                        passwordHasher.hash("email-code-account::" + normalizedEmail),
                        now,
                        now
                ));
                authAccountProvisioner.provisionAccount(account, deriveNickname(normalizedEmail));
            }
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            return new AuthSessionTokenResult(
                    account.id(),
                    tokenPair.accessToken(),
                    authTokenSettings.accessTokenTtl().toSeconds(),
                    tokenPair.refreshToken(),
                    newUser
            );
        });
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        if (!passwordLoginPolicy.enabled()) {
            throw ProblemException.forbidden("password_login_disabled", "password login is disabled");
        }
        AuthAccount account = authAccountRepository.findByUsername(command.username())
                .orElseThrow(() -> ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            throw ProblemException.forbidden("invalid_credentials", "username or password is invalid");
        }

        AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
        return toTokenResult(account, tokenPair);
    }

    @Override
    public AuthSessionTokenResult refreshTokenSession(RefreshTokenCommand command) {
        AuthTokenResult result = refresh(command);
        return new AuthSessionTokenResult(
                result.accountId(),
                result.accessToken(),
                authTokenSettings.accessTokenTtl().toSeconds(),
                result.refreshToken(),
                false
        );
    }

    @Override
    public void logout(LogoutCommand command) {
        transactionRunner.runInTransaction(() -> {
            ValidRefreshSession validSession = requireValidRefreshSession(command.refreshToken());
            authRefreshSessionRepository.revoke(validSession.session().id());
            return null;
        });
    }

    AuthTokenResult refresh(RefreshTokenCommand command) {
        return transactionRunner.runInTransaction(() -> {
            ValidRefreshSession validSession = requireValidRefreshSession(command.refreshToken());
            authRefreshSessionRepository.revoke(validSession.session().id());
            AuthAccount account = authAccountRepository.findById(validSession.accountId())
                    .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            return toTokenResult(account, tokenPair);
        });
    }

    private ValidRefreshSession requireValidRefreshSession(String refreshToken) {
        AuthTokenClaims claims = authTokenCodec.parseRefreshToken(refreshToken);
        long accountId = parseRefreshSubjectAccountId(claims);
        AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));

        if (session.revoked()
                || !session.expiresAt().isAfter(timeProvider.nowInstant())
                || session.accountId() != accountId
                || !tokenHasher.hash(refreshToken).equals(session.refreshTokenHash())) {
            throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
        }
        return new ValidRefreshSession(accountId, session);
    }

    /**
     * 从 refresh token claims 中解析账号 ID。
     * 失败语义：token subject 不是正数账号 ID 时返回非法 refresh token。
     *
     * @param claims 已校验的 refresh token claims
     * @return refresh token 归属账号 ID
     */
    private long parseRefreshSubjectAccountId(AuthTokenClaims claims) {
        try {
            long accountId = Long.parseLong(claims.subject());
            if (accountId <= 0L) {
                throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
            }
            return accountId;
        } catch (NumberFormatException exception) {
            throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String deriveNickname(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private AuthTokenResult toTokenResult(AuthAccount account, AuthTokenPair tokenPair) {
        return new AuthTokenResult(
                account.id(),
                account.username(),
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt(),
                authTokenSettings.accessTokenTtl().toSeconds(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }

    private record ValidRefreshSession(long accountId, AuthRefreshSession session) {
    }
}

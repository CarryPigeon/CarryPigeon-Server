package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

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
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
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
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AuthTokenCodec authTokenCodec;
    private final AuthTokenIssuer authTokenIssuer;
    private final AuthTokenSettings authTokenSettings;
    private final AuthPasswordLoginPolicy passwordLoginPolicy;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationApi emailVerificationApi;

    @Autowired
    public AuthSessionDomainApi(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenCodec authTokenCodec,
            AuthTokenSettings authTokenSettings,
            AuthPasswordLoginPolicy passwordLoginPolicy,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRefreshSessionRepository = authRefreshSessionRepository;
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
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationApi = emailVerificationApi;
    }

    @Override
    public AuthSessionTokenResult createTokenSession(CreateTokenSessionCommand command) {
        if (!"email_code".equals(command.grantType())) {
            throw ProblemException.validationFailed("grant_type must be email_code");
        }
        String email = command.email();
        emailVerificationApi.verifyCode(new VerifyEmailVerificationCodeCommand(email, command.code()));
        return transactionRunner.runInTransaction(() -> {
            AuthAccount account = authAccountRepository.findByEmail(email).orElse(null);
            if (account == null) {
                throw ProblemException.notFound("account_not_found");
            }
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            return new AuthSessionTokenResult(
                    account.id(),
                    tokenPair.accessToken(),
                    authTokenSettings.accessTokenTtl().toSeconds(),
                    tokenPair.refreshToken()
            );
        });
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        // 通过配置开关账号密码登录接口
        if (!passwordLoginPolicy.enabled()) {
            throw ProblemException.forbidden("password_login_disabled", "password login is disabled");
        }
        // 拿取用户信息
        AuthAccount account = authAccountRepository.findByUsername(command.username())
                .orElseThrow(() -> ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            throw ProblemException.forbidden("invalid_credentials", "username or password is invalid");
        }

        // 签发访问令牌和刷新令牌
        AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);

        // 响应并返回
        return toTokenResult(account, tokenPair);
    }

    @Override
    public AuthSessionTokenResult refreshTokenSession(RefreshTokenCommand command) {
        AuthTokenResult result = refresh(command);
        return new AuthSessionTokenResult(
                result.accountId(),
                result.accessToken(),
                authTokenSettings.accessTokenTtl().toSeconds(),
                result.refreshToken()
        );
    }

    @Override
    public void logout(LogoutCommand command) {
        transactionRunner.runInTransaction(() -> {
            ValidRefreshSession validSession = requireValidRefreshSession(command.refreshToken());
            if (!authRefreshSessionRepository.revokeIfActive(validSession.session().id())) {
                throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
            }
            return null;
        });
    }

    AuthTokenResult refresh(RefreshTokenCommand command) {
        return transactionRunner.runInTransaction(() -> {
            // 从token查询会话
            ValidRefreshSession validSession = requireValidRefreshSession(command.refreshToken());
            // 撤销会话
            authRefreshSessionRepository.revoke(validSession.session().id());
            // 查询用户
            AuthAccount account = authAccountRepository.findById(validSession.accountId())
                .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));
            // 创建新会话
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            // 返回最新的一组token
            return toTokenResult(account, tokenPair);
        });
    }

    private ValidRefreshSession requireValidRefreshSession(String refreshToken) {
        // 解析refresh令牌
        AuthTokenClaims claims = authTokenCodec.parseRefreshToken(refreshToken);
        // 获取用户id
        long accountId = parseRefreshSubjectAccountId(claims);
        // 查询会话是否真实存在
        AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid"));
        // 判断session是否可用
        if (session.revoked()
                || !session.expiresAt().isAfter(timeProvider.nowInstant())
                || session.accountId() != accountId
                || !tokenHasher.hash(refreshToken).equals(session.refreshTokenHash())) {
            throw ProblemException.forbidden("invalid_refresh_token", "refresh token is invalid");
        }
        // 返回对应的会话
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

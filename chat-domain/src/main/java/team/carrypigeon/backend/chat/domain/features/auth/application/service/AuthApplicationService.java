package team.carrypigeon.backend.chat.domain.features.auth.application.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.LogoutCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RefreshTokenCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.application.dto.RegisterResult;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenPair;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 鉴权应用服务。
 * 职责：编排注册、登录、token 刷新与刷新会话撤销用例。
 * 边界：当前阶段不承载验证码、OAuth、SSO 或复杂权限逻辑。
 */
@Service
public class AuthApplicationService {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AuthTokenService authTokenService;
    private final AuthJwtProperties authJwtProperties;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public AuthApplicationService(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserProfileRepository userProfileRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthJwtProperties authJwtProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.authTokenService = authTokenService;
        this.authJwtProperties = authJwtProperties;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    /**
     * 执行用户名密码注册。
     *
     * @param command 注册命令
     * @return 注册成功后的稳定结果
     */
    public RegisterResult register(RegisterCommand command) {
        return transactionRunner.runInTransaction(() -> {
            authAccountRepository.findByUsername(command.username())
                    .ifPresent(existing -> {
                        throw ProblemException.validationFailed("username already exists");
                    });

            AuthAccount account = new AuthAccount(
                    idGenerator.nextLongId(),
                    command.username(),
                    passwordHasher.hash(command.password()),
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );

            AuthAccount savedAccount = authAccountRepository.save(account);
            userProfileRepository.save(new UserProfile(
                    savedAccount.id(),
                    savedAccount.username(),
                    "",
                    "",
                    savedAccount.createdAt(),
                    savedAccount.updatedAt()
            ));
            return new RegisterResult(savedAccount.id(), savedAccount.username());
        });
    }

    /**
     * 执行用户名密码登录。
     *
     * @param command 登录命令
     * @return 登录成功后的稳定结果
     */
    public AuthTokenResult login(LoginCommand command) {
        AuthAccount account = authAccountRepository.findByUsername(command.username())
                .orElseThrow(() -> ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            throw ProblemException.forbidden("invalid_credentials", "username or password is invalid");
        }

        AuthTokenPair tokenPair = issueTokenPair(account);
        return toTokenResult(account, tokenPair);
    }

    /**
     * 刷新 access token 与 refresh token。
     *
     * @param command 刷新命令
     * @return 刷新后的 token 结果
     */
    public AuthTokenResult refresh(RefreshTokenCommand command) {
        return transactionRunner.runInTransaction(() -> {
            AuthTokenClaims claims = authTokenService.parseRefreshToken(command.refreshToken());
            long accountId = Long.parseLong(claims.subject());
            AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                    .orElseThrow(() -> ProblemException.forbidden("invalid_refresh_session", "refresh token is invalid"));

            if (session.revoked()
                    || session.expiresAt().isBefore(timeProvider.nowInstant())
                    || session.accountId() != accountId
                    || !tokenHasher.hash(command.refreshToken()).equals(session.refreshTokenHash())) {
                throw ProblemException.forbidden("invalid_refresh_session", "refresh token is invalid");
            }

            authRefreshSessionRepository.revoke(session.id());
            AuthAccount account = new AuthAccount(
                    accountId,
                    claims.username(),
                    "",
                    timeProvider.nowInstant(),
                    timeProvider.nowInstant()
            );
            AuthTokenPair tokenPair = issueTokenPair(account);
            return toTokenResult(account, tokenPair);
        });
    }

    /**
     * 注销并撤销 refresh session。
     *
     * @param command 注销命令
     */
    public void logout(LogoutCommand command) {
        AuthTokenClaims claims = authTokenService.parseRefreshToken(command.refreshToken());
        authRefreshSessionRepository.revoke(claims.sessionId());
    }

    private AuthTokenPair issueTokenPair(AuthAccount account) {
        long refreshSessionId = idGenerator.nextLongId();
        Instant accessTokenExpiresAt = timeProvider.nowInstant().plus(authJwtProperties.accessTokenTtl());
        Instant refreshTokenExpiresAt = timeProvider.nowInstant().plus(authJwtProperties.refreshTokenTtl());
        String accessToken = authTokenService.issueAccessToken(account, accessTokenExpiresAt);
        String refreshToken = authTokenService.issueRefreshToken(account, refreshSessionId, refreshTokenExpiresAt);

        authRefreshSessionRepository.save(new AuthRefreshSession(
                refreshSessionId,
                account.id(),
                tokenHasher.hash(refreshToken),
                refreshTokenExpiresAt,
                false,
                timeProvider.nowInstant(),
                timeProvider.nowInstant()
        ));

        return new AuthTokenPair(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, refreshSessionId);
    }

    private AuthTokenResult toTokenResult(AuthAccount account, AuthTokenPair tokenPair) {
        return new AuthTokenResult(
                account.id(),
                account.username(),
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}

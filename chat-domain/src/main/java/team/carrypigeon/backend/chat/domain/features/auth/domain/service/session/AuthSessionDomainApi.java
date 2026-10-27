package team.carrypigeon.backend.chat.domain.features.auth.domain.service.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenPair;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailVerificationApi;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 鉴权会话领域 API 实现。
 * 职责：直接承载登录、验证码会话、刷新和注销用例实现。
 * 边界：不负责账号注册和验证码发送入口。
 */
@Service
public class AuthSessionDomainApi implements AuthTokenApi {

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
    public AuthTokenResult loginByPWD(String username,String password) {
        // 通过配置开关账号密码登录接口
        if (!passwordLoginPolicy.enabled()) {
            throw ProblemException.of(AuthProblem.PASSWORD_LOGIN_DISABLED);
        }
        // 拿取用户信息
        AuthAccount account = authAccountRepository.findByUsername(username)
                .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_CREDENTIALS));

        if (!passwordHasher.matches(password, account.passwordHash())) {
            throw ProblemException.of(AuthProblem.INVALID_CREDENTIALS);
        }

        // 签发访问令牌和刷新令牌
        AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);

        // 响应并返回
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

    @Override
    public AuthTokenResult loginByEmail(String email, String password) {
        // 拿取用户信息
        AuthAccount account = authAccountRepository.findByEmail(email)
            .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_CREDENTIALS));
        emailVerificationApi.verifyCode(email, password);
        // 签发访问令牌和刷新令牌
        AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
        // 响应并返回
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

    @Override
    public AccessTokenAuthenticationResult authenticate(String accessToken) {
        AuthTokenClaims claims = authTokenCodec.parseAccessToken(accessToken);
        return new AccessTokenAuthenticationResult(
            Long.parseLong(claims.subject()),
            claims.expiresAt()
        );
    }
    @Override
    public AuthTokenResult refreshToken(String refreshToken) {
        return transactionRunner.runInTransaction(() -> {
            // 解析refresh令牌
            AuthTokenClaims claims = authTokenCodec.parseRefreshToken(refreshToken);
            // 获取用户id
            long accountId = Long.parseLong(claims.subject());
            // 查询会话是否真实存在
            AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN));
            // 判断session是否可用
            if (session.revoked()
                || !session.expiresAt().isAfter(timeProvider.nowInstant())
                || session.accountId() != accountId
                || !tokenHasher.hash(refreshToken).equals(session.refreshTokenHash())) {
                throw ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN);
            }
            // 撤销会话
            authRefreshSessionRepository.revoke(session.id());
            // 查询用户
            AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN));
            // 创建新会话
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            // 返回最新的一组token
            return new AuthTokenResult(
                account.id(),
                account.username(),
                tokenPair.accessToken(),
                tokenPair.accessTokenExpiresAt(),
                authTokenSettings.accessTokenTtl().toSeconds(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
            );
        });
    }

    @Override
    public void logout(String refreshToken) {
        transactionRunner.runInTransaction(() -> {
            // 解析refresh令牌
            AuthTokenClaims claims = authTokenCodec.parseRefreshToken(refreshToken);
            // 获取用户id
            long accountId = Long.parseLong(claims.subject());
            // 查询会话是否真实存在
            AuthRefreshSession session = authRefreshSessionRepository.findById(claims.sessionId())
                .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN));
            // 判断session是否可用
            if (session.revoked()
                || !session.expiresAt().isAfter(timeProvider.nowInstant())
                || session.accountId() != accountId
                || !tokenHasher.hash(refreshToken).equals(session.refreshTokenHash())) {
                throw ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN);
            }
            if (!authRefreshSessionRepository.revokeIfActive(session.id())) {
                throw ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN);
            }
        });
    }
}

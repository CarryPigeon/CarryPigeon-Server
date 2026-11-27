package team.carrypigeon.backend.chat.domain.features.auth.domain.service.session;

import java.util.Locale;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenClaims;
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
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.chat.domain.shared.transaction.TransactionRunner;

/**
 * 鉴权会话领域 API 实现。
 * 职责：直接承载登录、验证码会话、刷新和注销用例实现。
 * 边界：不负责账号注册和验证码发送入口。
 */
@Slf4j
@Service
public class AuthSessionService implements AuthTokenApi {

    private final AuthAccountRepository authAccountRepository;
    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final AuthTokenCodec authTokenCodec;
    private final AuthTokenIssuer authTokenIssuer;
    private final AuthTokenSettings authTokenSettings;
    private final AuthPasswordLoginPolicy passwordLoginPolicy;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final EmailVerificationApi emailVerificationApi;
    private final PasswordLoginLimiter passwordLoginLimiter;

    @Autowired
    public AuthSessionService(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenCodec authTokenCodec,
            AuthTokenSettings authTokenSettings,
            AuthTokenIssuer authTokenIssuer,
            AuthPasswordLoginPolicy passwordLoginPolicy,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi,
            PasswordLoginLimiter passwordLoginLimiter
    ) {
        this.authAccountRepository = authAccountRepository;
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.authTokenCodec = authTokenCodec;
        this.authTokenIssuer = authTokenIssuer;
        this.authTokenSettings = authTokenSettings;
        this.passwordLoginPolicy = passwordLoginPolicy;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.emailVerificationApi = emailVerificationApi;
        this.passwordLoginLimiter = passwordLoginLimiter;
    }

    @Override
    public AuthTokenResult loginByPWD(String username, String password, String clientIp) {
        try {
            // 通过配置开关账号密码登录接口
            if (!passwordLoginPolicy.enabled()) {
                throw ProblemException.of(AuthProblem.PASSWORD_LOGIN_DISABLED);
            }
            passwordLoginLimiter.acquireIp(clientIp);
            String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
            AuthAccount account = authAccountRepository.findByUsername(username.trim()).orElse(null);
            // 在计算昂贵的密码哈希前预占账号名额，未知账号也保留失败配额。
            PasswordLoginLimiter.Attempt attempt = passwordLoginLimiter.reserveAccount(account == null
                    ? "username:" + normalizedUsername : "account:" + account.id());
            if (account == null || !passwordHasher.matches(password, account.passwordHash())) {
                throw ProblemException.of(AuthProblem.INVALID_CREDENTIALS);
            }
            passwordLoginLimiter.releaseSuccessful(attempt);

            // 签发访问令牌和刷新令牌
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);

            log.atInfo()
                .addKeyValue("action", "auth.login")
                .addKeyValue("outcome", "success")
                .addKeyValue("account_id", account.id())
                .addKeyValue("method", "password")
                .log("登录成功");
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
        } catch (ProblemException exception) {
            if (exception.mapping() == AuthProblem.INVALID_CREDENTIALS || exception.mapping() == AuthProblem.LOGIN_RATE_LIMITED) {
                log.atInfo()
                    .addKeyValue("action", "auth.login")
                    .addKeyValue("outcome", "failure")
                    .addKeyValue("method", "password")
                    .addKeyValue("business_code", exception.mapping().code())
                    .log("登录被拒绝");
            }
            throw exception;
        }
    }

    @Override
    public AuthTokenResult loginByEmail(String email, String password) {
        try {
            // 拿取用户信息
            AuthAccount account = authAccountRepository.findByEmail(email)
                .orElseThrow(() -> ProblemException.of(AuthProblem.INVALID_CREDENTIALS));
            emailVerificationApi.verifyCode(email, password, EmailCodePurpose.LOGIN, null);
            // 签发访问令牌和刷新令牌
            AuthTokenPair tokenPair = authTokenIssuer.issueTokenPair(account);
            log.atInfo()
                .addKeyValue("action", "auth.login")
                .addKeyValue("outcome", "success")
                .addKeyValue("account_id", account.id())
                .addKeyValue("method", "email")
                .log("登录成功");
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
        } catch (ProblemException exception) {
            if (exception.mapping() == AuthProblem.INVALID_CREDENTIALS
                    || exception.mapping() == VerificationProblem.INVALID_CODE || exception.mapping() == VerificationProblem.TOO_MANY_REQUESTS) {
                log.atInfo()
                    .addKeyValue("action", "auth.login")
                    .addKeyValue("outcome", "failure")
                    .addKeyValue("method", "email")
                    .addKeyValue("business_code", exception.mapping().code())
                    .log("登录被拒绝");
            }
            throw exception;
        }
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
            // 条件更新抢占旧令牌的消费权；与新会话写入共用事务，签发失败时一并回滚。
            if (!authRefreshSessionRepository.revokeIfActive(session.id())) {
                throw ProblemException.of(AuthProblem.INVALID_REFRESH_TOKEN);
            }
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
        transactionRunner.runInTransaction(afterCommit -> {
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
            afterCommit.execute(
                () -> log.atInfo()
                    .addKeyValue("action", "auth.revoke")
                    .addKeyValue("outcome", "success")
                    .addKeyValue("account_id", accountId)
                    .log("登录会话已撤销")
            );
        });
    }
}

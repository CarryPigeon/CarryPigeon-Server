package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthRefreshSession;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenPair;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 鉴权 token 签发协作对象。
 * 职责：生成 access/refresh token pair，并持久化 refresh session。
 * 边界：不校验登录凭据、不校验 refresh token 是否有效、不处理事务边界。
 */
class AuthTokenIssuer {

    private final AuthRefreshSessionRepository authRefreshSessionRepository;
    private final TokenHasher tokenHasher;
    private final AuthTokenService authTokenService;
    private final AuthTokenSettings authTokenSettings;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    AuthTokenIssuer(
            AuthRefreshSessionRepository authRefreshSessionRepository,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthTokenSettings authTokenSettings,
            IdGenerator idGenerator,
            TimeProvider timeProvider
    ) {
        this.authRefreshSessionRepository = authRefreshSessionRepository;
        this.tokenHasher = tokenHasher;
        this.authTokenService = authTokenService;
        this.authTokenSettings = authTokenSettings;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 为账号签发 access/refresh token pair。
     * 输入：已完成认证或建号流程的账号。
     * 输出：明文 token、过期时间和 refresh session 标识。
     * 副作用：持久化 refresh token 哈希值，后续刷新流程只校验哈希后的 refresh token。
     *
     * @param account 需要签发会话令牌的账号
     * @return 新签发的 token pair 与 refresh session 信息
     */
    AuthTokenPair issueTokenPair(AuthAccount account) {
        long refreshSessionId = idGenerator.nextLongId();
        Instant accessTokenExpiresAt = timeProvider.nowInstant().plus(authTokenSettings.accessTokenTtl());
        Instant refreshTokenExpiresAt = timeProvider.nowInstant().plus(authTokenSettings.refreshTokenTtl());
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
}

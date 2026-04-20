package team.carrypigeon.backend.chat.domain.features.auth.domain.service;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;

/**
 * 鉴权令牌服务。
 * 职责：签发并解析本服务端内有效的 JWT。
 * 边界：不负责 refresh session 持久化与业务用例编排。
 */
public interface AuthTokenService {

    /**
     * 签发访问令牌。
     *
     * @param account 账户
     * @param expiresAt 过期时间
     * @return JWT access token
     */
    String issueAccessToken(AuthAccount account, Instant expiresAt);

    /**
     * 签发刷新令牌。
     *
     * @param account 账户
     * @param refreshSessionId 刷新会话 ID
     * @param expiresAt 过期时间
     * @return JWT refresh token
     */
    String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt);

    /**
     * 解析并校验访问令牌。
     *
     * @param accessToken access token
     * @return 令牌声明
     */
    AuthTokenClaims parseAccessToken(String accessToken);

    /**
     * 解析并校验刷新令牌。
     *
     * @param refreshToken refresh token
     * @return 令牌声明
     */
    AuthTokenClaims parseRefreshToken(String refreshToken);
}

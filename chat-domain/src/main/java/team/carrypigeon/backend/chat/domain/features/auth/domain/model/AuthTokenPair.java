package team.carrypigeon.backend.chat.domain.features.auth.domain.model;

import java.time.Instant;

/**
 * 鉴权令牌对。
 * 职责：表达 access token 与 refresh token 的签发结果。
 * 边界：不承载协议响应字段命名与持久化细节。
 *
 * @param accessToken 访问令牌
 * @param accessTokenExpiresAt 访问令牌过期时间
 * @param refreshToken 刷新令牌
 * @param refreshTokenExpiresAt 刷新令牌过期时间
 * @param refreshSessionId 刷新会话 ID
 */
public record AuthTokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        long refreshSessionId
) {
}

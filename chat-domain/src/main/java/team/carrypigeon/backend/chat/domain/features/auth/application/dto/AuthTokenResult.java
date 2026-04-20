package team.carrypigeon.backend.chat.domain.features.auth.application.dto;

import java.time.Instant;

/**
 * 鉴权令牌应用结果。
 * 职责：向协议层返回账户标识与 token 签发结果。
 * 边界：不承载客户端存储或跨服务端身份语义。
 *
 * @param accountId 账户 ID
 * @param username 用户名
 * @param accessToken access token
 * @param accessTokenExpiresAt access token 过期时间
 * @param refreshToken refresh token
 * @param refreshTokenExpiresAt refresh token 过期时间
 */
public record AuthTokenResult(
        long accountId,
        String username,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}

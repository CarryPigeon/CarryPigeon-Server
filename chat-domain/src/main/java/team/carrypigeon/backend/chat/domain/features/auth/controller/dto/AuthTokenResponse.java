package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import java.time.Instant;

/**
 * 鉴权令牌响应。
 * 职责：向调用方返回登录或刷新后的最小 token 数据。
 * 边界：不包含权限、角色或服务端内部会话状态。
 *
 * @param accountId 账户 ID
 * @param username 用户名
 * @param accessToken access token
 * @param accessTokenExpiresAt access token 过期时间
 * @param refreshToken refresh token
 * @param refreshTokenExpiresAt refresh token 过期时间
 */
public record AuthTokenResponse(
        long accountId,
        String username,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}

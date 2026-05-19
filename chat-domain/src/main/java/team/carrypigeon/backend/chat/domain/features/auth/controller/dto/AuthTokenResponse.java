package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "账户 ID", example = "1001")
        long accountId,
        @Schema(description = "用户名", example = "carry_user")
        String username,
        @Schema(description = "访问令牌，用于请求受保护 HTTP 接口", example = "eyJhbGciOiJIUzI1NiJ9.access.token")
        String accessToken,
        @Schema(description = "access token 过期时间", example = "2026-05-14T12:00:00Z")
        Instant accessTokenExpiresAt,
        @Schema(description = "刷新令牌，用于换取新的 access token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        String refreshToken,
        @Schema(description = "refresh token 过期时间", example = "2026-05-28T12:00:00Z")
        Instant refreshTokenExpiresAt
) {
}

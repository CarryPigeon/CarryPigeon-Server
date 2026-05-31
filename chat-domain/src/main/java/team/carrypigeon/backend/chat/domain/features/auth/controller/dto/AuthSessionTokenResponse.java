package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 会话令牌响应。
 * 职责：向客户端返回 v1 会话令牌签发结果。
 * 边界：只承载 token 与最小用户标识，不扩展资料字段。
 */
public record AuthSessionTokenResponse(
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9.access.token")
        String accessToken,
        @Schema(description = "访问令牌剩余有效秒数", example = "1800")
        long expiresIn,
        @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        String refreshToken,
        @Schema(description = "当前用户 ID", example = "1001")
        String uid,
        @Schema(description = "是否为首次创建的新用户", example = "false")
        boolean isNewUser
) {
}

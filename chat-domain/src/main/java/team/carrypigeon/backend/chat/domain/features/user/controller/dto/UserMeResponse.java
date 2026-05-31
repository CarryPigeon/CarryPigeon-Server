package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户响应。
 * 职责：承载 `GET /api/users/me` 的最小当前用户信息。
 * 边界：只返回 v1 当前用户字段，不暴露资料扩展与令牌信息。
 */
public record UserMeResponse(
        @Schema(description = "用户 ID", example = "1001")
        String uid,
        @Schema(description = "当前邮箱", example = "user@example.com")
        String email,
        @Schema(description = "用户昵称", example = "Alice")
        String nickname,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
        String avatar
) {
}

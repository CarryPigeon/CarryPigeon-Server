package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户响应。
 * 职责：承载 `GET /api/users/me` 的账号与公开资料组合结果。
 */
public record CurrentUserResponse(
        @Schema(description = "用户 ID", example = "1001") String uid,
        @Schema(description = "当前邮箱", example = "user@example.com") String email,
        @Schema(description = "用户昵称", example = "Alice") String nickname,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png") String avatar
) {
}

package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserDetailProfileResponse(
    @Schema(description = "用户 ID", example = "1001")
    String uid,
    @Schema(description = "用户名", example = "Alice")
    String username,
    @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
    String avatar,
    @Schema(description = "用户邮箱", example = "alice@example.com")
    String email,
    @Schema(description = "用户简介", example = "一个热爱编程的开发者")
    String bio,
    @Schema(description = "用户性别", example = "1")
    Long sex,
    @Schema(description = "用户生日", example = "1990-01-01")
    Long birthday
) {
}

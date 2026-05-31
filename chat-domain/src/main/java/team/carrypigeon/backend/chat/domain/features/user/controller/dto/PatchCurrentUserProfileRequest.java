package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PATCH 当前用户资料请求。
 * 职责：承载 `PATCH /api/users/me` 的 v1 最小字段。
 * 边界：当前仅收敛到可由现有资料模型承接的字段。
 */
public record PatchCurrentUserProfileRequest(
        @Schema(description = "新的展示名", example = "Alice")
        @NotBlank(message = "username must not be blank")
        @Size(max = 64, message = "username length must be less than or equal to 64")
        String username,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
        @NotNull(message = "avatar must not be null")
        @Size(max = 512, message = "avatar length must be less than or equal to 512")
        String avatar,
        @Schema(description = "个人简介", example = "hello")
        @NotNull(message = "brief must not be null")
        @Size(max = 1024, message = "brief length must be less than or equal to 1024")
        String brief,
        @Schema(description = "保留字段：性别", example = "0")
        Long sex,
        @Schema(description = "保留字段：生日", example = "0")
        Long birthday
) {
}

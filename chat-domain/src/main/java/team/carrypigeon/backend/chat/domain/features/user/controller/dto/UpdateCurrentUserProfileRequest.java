package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新当前用户资料请求。
 * 职责：承载 HTTP 用户资料更新入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载资料更新业务编排。
 *
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 */
public record UpdateCurrentUserProfileRequest(
        @Schema(description = "用户昵称", example = "Carry Pigeon")
        @NotBlank(message = "nickname must not be blank")
        @Size(max = 64, message = "nickname length must be less than or equal to 64")
        String nickname,
        @Schema(description = "头像地址；可为空串表示清空头像", example = "https://cdn.example.com/avatar.png")
        @NotNull(message = "avatarUrl must not be null")
        @Size(max = 512, message = "avatarUrl length must be less than or equal to 512")
        String avatarUrl,
        @Schema(description = "个人简介；可为空串", example = "Backend developer and pigeon lover")
        @NotNull(message = "bio must not be null")
        @Size(max = 1024, message = "bio length must be less than or equal to 1024")
        String bio
) {
}

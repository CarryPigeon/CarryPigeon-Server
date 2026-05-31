package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户公开资料响应。
 * 职责：承载公共用户资源读取所需的最小字段。
 * 边界：不暴露邮箱、简介与时间字段。
 */
public record UserPublicProfileResponse(
        @Schema(description = "用户 ID", example = "1001")
        String uid,
        @Schema(description = "用户昵称", example = "Alice")
        String nickname,
        @Schema(description = "用户头像相对路径", example = "avatars/u/1001.png")
        String avatar
) {
}

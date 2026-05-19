package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 用户资料响应。
 * 职责：对外返回当前登录用户的稳定资料信息。
 * 边界：不暴露鉴权口令、令牌或权限状态。
 *
 * @param accountId 账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfileResponse(
        @Schema(description = "账户 ID", example = "1001")
        long accountId,
        @Schema(description = "用户昵称", example = "Carry Pigeon")
        String nickname,
        @Schema(description = "用户头像地址", example = "https://cdn.example.com/avatar.png")
        String avatarUrl,
        @Schema(description = "用户简介", example = "Backend developer and pigeon lover")
        String bio,
        @Schema(description = "创建时间", example = "2026-05-01T08:00:00Z")
        Instant createdAt,
        @Schema(description = "更新时间", example = "2026-05-13T08:00:00Z")
        Instant updatedAt
) {
}

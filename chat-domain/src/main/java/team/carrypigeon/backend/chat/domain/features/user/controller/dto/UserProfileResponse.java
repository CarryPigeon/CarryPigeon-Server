package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

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
        long accountId,
        String nickname,
        String avatarUrl,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
}

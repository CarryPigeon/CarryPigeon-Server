package team.carrypigeon.backend.chat.domain.features.user.domain.model;

import java.time.Instant;

/**
 * 用户资料领域模型。
 * 职责：表达已持久化的当前用户资料语义。
 * 边界：不承载鉴权口令、令牌或角色权限语义。
 *
 * @param accountId 关联鉴权账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfile(
        long accountId,
        String nickname,
        String avatarUrl,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
}

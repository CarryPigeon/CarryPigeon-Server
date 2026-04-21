package team.carrypigeon.backend.chat.domain.features.user.application.dto;

import java.time.Instant;

/**
 * 用户资料结果。
 * 职责：返回当前用户资料查询与更新用例的稳定结果。
 * 边界：不包含鉴权口令、令牌或权限信息。
 *
 * @param accountId 账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfileResult(
        long accountId,
        String nickname,
        String avatarUrl,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
}

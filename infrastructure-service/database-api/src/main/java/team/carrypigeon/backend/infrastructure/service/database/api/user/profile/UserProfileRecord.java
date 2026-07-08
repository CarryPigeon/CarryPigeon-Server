package team.carrypigeon.backend.infrastructure.service.database.api.user.profile;

import java.time.Instant;

/**
 * 用户资料数据库记录契约。
 * 职责：表达用户资料持久化所需的最小统一投影字段。
 * 边界：同时服务查询与更新契约，不承载用户资料业务决策语义。
 *
 * @param accountId 关联账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param sex 用户性别协议值
 * @param birthday 用户生日协议值
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfileRecord(
        long accountId,
        String nickname,
        String avatarUrl,
        String bio,
        long sex,
        long birthday,
        Instant createdAt,
        Instant updatedAt
) {
}

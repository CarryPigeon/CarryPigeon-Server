package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 用户资料数据库记录契约。
 * 职责：为 chat-domain 与 database-impl 之间传递用户资料持久化字段。
 * 边界：这里只表达数据库服务契约，不承载用户资料业务规则与协议语义。
 *
 * @param accountId 关联账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfileRecord(
        long accountId,
        String nickname,
        String avatarUrl,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
}

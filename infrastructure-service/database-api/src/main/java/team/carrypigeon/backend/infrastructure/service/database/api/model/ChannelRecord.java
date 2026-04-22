package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道数据库记录契约。
 * 职责：为 chat-domain 与 database-impl 之间传递频道持久化字段。
 * 边界：这里只表达数据库服务契约，不承载频道业务规则。
 *
 * @param id 频道 ID
 * @param conversationId 会话 ID
 * @param name 频道名称
 * @param type 频道类型
 * @param defaultChannel 是否为默认频道
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelRecord(
        long id,
        long conversationId,
        String name,
        String type,
        boolean defaultChannel,
        Instant createdAt,
        Instant updatedAt
) {
}

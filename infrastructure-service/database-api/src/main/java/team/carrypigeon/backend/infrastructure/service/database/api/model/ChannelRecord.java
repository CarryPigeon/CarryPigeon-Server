package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道数据库记录契约。
 * 职责：表达频道读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载额外业务语义。
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

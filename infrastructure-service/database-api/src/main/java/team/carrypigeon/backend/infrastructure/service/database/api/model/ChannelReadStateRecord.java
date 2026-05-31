package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道已读状态数据库记录契约。
 */
public record ChannelReadStateRecord(
        long channelId,
        long accountId,
        long lastReadMessageId,
        Instant lastReadTime,
        Instant createdAt,
        Instant updatedAt
) {
}

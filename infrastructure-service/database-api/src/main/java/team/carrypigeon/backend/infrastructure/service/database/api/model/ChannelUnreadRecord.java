package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道未读数据库投影。
 */
public record ChannelUnreadRecord(long channelId, long unreadCount, Instant lastReadTime) {
}

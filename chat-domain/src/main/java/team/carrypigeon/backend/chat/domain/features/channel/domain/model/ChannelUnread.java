package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道未读领域投影。
 */
public record ChannelUnread(long channelId, long unreadCount, Instant lastReadTime) {
}

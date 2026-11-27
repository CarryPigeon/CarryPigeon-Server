package team.carrypigeon.backend.chat.domain.features.message.domain.model;

/**
 * 频道未读领域投影；unreadCount 上限为 10000，达到上限后不再继续精确统计。
 */
public record ChannelUnread(long channelId, long unreadCount) {
}

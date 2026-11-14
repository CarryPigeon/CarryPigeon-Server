package team.carrypigeon.backend.chat.domain.features.message.domain.model;

/**
 * 频道未读领域投影。
 */
public record ChannelUnread(long channelId, long unreadCount) {
}

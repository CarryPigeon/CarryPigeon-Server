package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result;

/**
 * 频道未读结果。
 *
 * @param channelId 频道 ID
 * @param unreadCount 未读数量
 */
public record ChannelUnreadResult(long channelId, long unreadCount) {
}

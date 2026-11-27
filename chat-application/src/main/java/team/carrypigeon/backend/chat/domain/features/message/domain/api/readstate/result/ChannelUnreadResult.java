package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result;

/**
 * 频道未读结果。
 *
 * @param channelId 频道 ID
 * @param unreadCount 未读数量，范围 1..10000；10000 表示达到统计上限
 */
public record ChannelUnreadResult(long channelId, long unreadCount) {
}

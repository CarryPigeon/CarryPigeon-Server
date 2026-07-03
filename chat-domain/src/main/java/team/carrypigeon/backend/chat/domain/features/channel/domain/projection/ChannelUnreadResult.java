package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 频道未读结果。
 *
 * @param cid 频道 ID
 * @param unreadCount 未读数量
 * @param lastReadTime 最后已读时间（epoch 毫秒）
 */
public record ChannelUnreadResult(String cid, long unreadCount, long lastReadTime) {
}

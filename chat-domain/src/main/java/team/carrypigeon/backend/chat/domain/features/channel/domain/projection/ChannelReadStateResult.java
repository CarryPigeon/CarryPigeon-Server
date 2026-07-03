package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 频道已读状态结果。
 *
 * @param cid 频道 ID
 * @param uid 账户 ID
 * @param lastReadMid 最后已读消息 ID
 * @param lastReadTime 最后已读时间（epoch 毫秒）
 */
public record ChannelReadStateResult(String cid, String uid, String lastReadMid, long lastReadTime) {
}

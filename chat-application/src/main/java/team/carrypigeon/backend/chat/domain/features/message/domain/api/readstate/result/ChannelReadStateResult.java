package team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result;

/**
 * 频道已读状态结果。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param lastReadMessageId 最后已读消息 ID
 */
public record ChannelReadStateResult(long channelId, long accountId, long lastReadMessageId) {
}

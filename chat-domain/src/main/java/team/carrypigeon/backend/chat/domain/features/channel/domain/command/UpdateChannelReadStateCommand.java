package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

/**
 * 更新频道已读状态命令。
 *
 * @param accountId 账户 ID
 * @param channelId 频道 ID
 * @param lastReadMessageId 最后已读消息 ID
 * @param lastReadTime 最后已读时间（epoch 毫秒）
 */
public record UpdateChannelReadStateCommand(long accountId, long channelId, long lastReadMessageId, long lastReadTime) {
}

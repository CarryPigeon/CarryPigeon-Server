package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * 取消置顶频道消息命令。
 */
public record UnpinChannelMessageCommand(long accountId, long channelId, long messageId) {
}

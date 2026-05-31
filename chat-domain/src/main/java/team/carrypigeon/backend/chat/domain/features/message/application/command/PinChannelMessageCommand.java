package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * 置顶频道消息命令。
 */
public record PinChannelMessageCommand(long accountId, long channelId, long messageId, String note) {
}

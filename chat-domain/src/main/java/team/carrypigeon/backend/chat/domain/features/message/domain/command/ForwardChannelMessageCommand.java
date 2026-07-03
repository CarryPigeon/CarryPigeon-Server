package team.carrypigeon.backend.chat.domain.features.message.domain.command;

/**
 * 转发频道消息命令。
 */
public record ForwardChannelMessageCommand(long accountId, long sourceMessageId, long targetChannelId, String comment) {
}

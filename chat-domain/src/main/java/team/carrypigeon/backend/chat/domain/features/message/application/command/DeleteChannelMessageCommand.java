package team.carrypigeon.backend.chat.domain.features.message.application.command;

/**
 * 频道消息硬删除命令。
 */
public record DeleteChannelMessageCommand(long accountId, long messageId) {
}

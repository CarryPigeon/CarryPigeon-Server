package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 删除频道命令。
 */
public record DeleteChannelCommand(long operatorAccountId, long channelId) {
}

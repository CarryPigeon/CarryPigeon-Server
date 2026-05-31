package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 创建入群申请命令。
 */
public record CreateChannelApplicationCommand(long accountId, long channelId, String reason) {
}

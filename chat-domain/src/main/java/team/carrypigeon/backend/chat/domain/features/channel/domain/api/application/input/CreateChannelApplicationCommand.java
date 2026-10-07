package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input;



/**
 * 创建入群申请命令。
 */
public record CreateChannelApplicationCommand(long accountId, long channelId, String reason) {
}

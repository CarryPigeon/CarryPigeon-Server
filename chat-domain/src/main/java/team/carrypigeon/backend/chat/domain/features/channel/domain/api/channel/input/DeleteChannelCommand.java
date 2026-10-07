package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input;



/**
 * 删除频道命令。
 */
public record DeleteChannelCommand(long operatorAccountId, long channelId) {
}

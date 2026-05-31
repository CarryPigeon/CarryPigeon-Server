package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 审批入群申请命令。
 */
public record DecideChannelApplicationCommand(long operatorAccountId, long channelId, long applicationId, String decision) {
}

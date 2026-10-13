package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input;



/**
 * 审批入群申请命令。
 */
public record DecideChannelApplicationCommand(long operatorAccountId, long applicationId, String decision) {
}

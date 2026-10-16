package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input;



/**
 * 审批入群申请命令。
 * 职责：承载审批用例所需的最小输入，包含申请所属频道以约束资源路径归属。
 * 边界：只表达应用层命令，不承载审批权限与状态流转规则。
 *
 * @param operatorAccountId 审批操作者账号 ID
 * @param channelId 申请所属频道 ID
 * @param applicationId 待审批申请 ID
 * @param decision 审批决定，仅接受 approve 或 reject
 */
public record DecideChannelApplicationCommand(
        long operatorAccountId,
        long channelId,
        long applicationId,
        String decision
) {
}

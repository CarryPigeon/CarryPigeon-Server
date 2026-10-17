package team.carrypigeon.backend.chat.domain.features.channel.domain.api.application.input;



/**
 * 创建入群申请命令。
 * 职责：承载提交入群申请用例所需的最小输入。
 * 边界：只表达应用层命令，不承载成员校验、重复申请判定与申请状态流转规则。
 *
 * @param accountId 提交申请的账号 ID
 * @param channelId 申请加入的频道 ID
 * @param reason 申请理由；可为空，落库前统一归一为空字符串
 */
public record CreateChannelApplicationCommand(long accountId, long channelId, String reason) {
}

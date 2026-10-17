package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input;



/**
 * 删除频道命令。
 * 职责：承载删除频道用例所需的最小输入。
 * 边界：只表达应用层命令，不承载所有者校验与依赖数据检查规则。
 *
 * @param operatorAccountId 执行删除的账号 ID
 * @param channelId 待删除频道 ID
 */
public record DeleteChannelCommand(long operatorAccountId, long channelId) {
}

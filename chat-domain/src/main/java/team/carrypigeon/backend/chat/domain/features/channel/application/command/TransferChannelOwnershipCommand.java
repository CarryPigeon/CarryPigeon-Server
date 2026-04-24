package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 转移频道所有权命令。
 * 职责：承载频道 OWNER 转移用例所需的最小输入。
 * 边界：只表达应用层命令，不承载治理规则判断。
 *
 * @param operatorAccountId 当前 OWNER 账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 新 OWNER 账户 ID
 */
public record TransferChannelOwnershipCommand(long operatorAccountId, long channelId, long targetAccountId) {
}

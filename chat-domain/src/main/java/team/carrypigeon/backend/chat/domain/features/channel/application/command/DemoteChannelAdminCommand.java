package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 降级频道 ADMIN 为 MEMBER 命令。
 * 职责：承载频道管理员降级用例所需的最小输入。
 * 边界：只表达应用层命令，不承载治理规则判断。
 *
 * @param operatorAccountId 操作人账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 目标管理员账户 ID
 */
public record DemoteChannelAdminCommand(long operatorAccountId, long channelId, long targetAccountId) {
}

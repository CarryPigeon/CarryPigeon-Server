package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 禁言频道成员命令。
 * 职责：承载频道成员禁言用例所需的最小输入。
 * 边界：只表达应用层命令，不承载治理规则判断。
 *
 * @param operatorAccountId 操作人账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 目标成员账户 ID
 * @param durationSeconds 禁言持续秒数
 */
public record MuteChannelMemberCommand(long operatorAccountId, long channelId, long targetAccountId, long durationSeconds) {
}

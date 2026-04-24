package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 封禁频道成员命令。
 * 职责：承载频道成员封禁用例所需的最小输入。
 * 边界：只表达应用层命令，不承载治理规则判断。
 *
 * @param operatorAccountId 操作人账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 目标成员账户 ID
 * @param reason 封禁原因
 * @param durationSeconds 封禁持续秒数；为空表示无限期
 */
public record BanChannelMemberCommand(
        long operatorAccountId,
        long channelId,
        long targetAccountId,
        String reason,
        Long durationSeconds
) {
}

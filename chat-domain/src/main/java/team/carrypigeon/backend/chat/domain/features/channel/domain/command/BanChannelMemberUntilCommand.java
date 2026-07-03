package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

/**
 * 按绝对过期时间封禁频道成员命令。
 * 职责：承载协议层以毫秒时间戳表达的封禁截止时间。
 * 边界：不在协议入口计算持续时间，由领域服务基于统一时间源转换为内部封禁时长。
 *
 * @param operatorAccountId 操作人账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 目标成员账户 ID
 * @param reason 封禁原因
 * @param untilEpochMillis 封禁截止毫秒时间戳；为空表示无限期
 */
public record BanChannelMemberUntilCommand(
        long operatorAccountId,
        long channelId,
        long targetAccountId,
        String reason,
        Long untilEpochMillis
) {
}

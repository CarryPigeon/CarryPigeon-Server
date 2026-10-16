package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input;



/**
 * 按绝对过期时间禁言频道成员命令。
 * 职责：承载协议层以毫秒时间戳表达的禁言截止时间。
 * 边界：不在协议入口计算持续时间，由领域服务基于统一时间源转换为内部禁言时长。
 *
 * @param operatorAccountId 操作人账户 ID
 * @param channelId 频道 ID
 * @param targetAccountId 目标成员账户 ID
 * @param reason 禁言原因
 * @param untilEpochMillis 禁言截止毫秒时间戳；为空表示不设置自动到期
 */
public record BanChannelMemberUntilCommand(
        long operatorAccountId,
        long channelId,
        long targetAccountId,
        String reason,
        Long untilEpochMillis
) {
}

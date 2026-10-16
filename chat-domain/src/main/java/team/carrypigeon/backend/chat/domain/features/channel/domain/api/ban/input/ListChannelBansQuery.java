package team.carrypigeon.backend.chat.domain.features.channel.domain.api.ban.input;



/**
 * 查询频道禁言列表。
 * 职责：承载查看频道禁言记录所需的最小输入。
 * 边界：只表达应用层查询参数，不承载治理规则判断。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 */
public record ListChannelBansQuery(long accountId, long channelId) {
}

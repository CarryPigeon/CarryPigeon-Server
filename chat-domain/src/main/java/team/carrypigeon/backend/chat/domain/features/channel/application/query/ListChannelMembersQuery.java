package team.carrypigeon.backend.chat.domain.features.channel.application.query;

/**
 * 查询频道成员列表参数。
 * 职责：承载活跃成员查看频道成员列表所需的最小输入。
 * 边界：只表达应用层查询参数，不承载治理规则判断。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 */
public record ListChannelMembersQuery(long accountId, long channelId) {
}

package team.carrypigeon.backend.chat.domain.features.channel.application.query;

/**
 * 查询入群申请列表。
 */
public record ListChannelApplicationsQuery(long accountId, long channelId) {
}

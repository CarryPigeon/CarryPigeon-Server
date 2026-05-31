package team.carrypigeon.backend.chat.domain.features.channel.application.query;

/**
 * 查询频道封禁列表。
 */
public record ListChannelBansQuery(long accountId, long channelId) {
}

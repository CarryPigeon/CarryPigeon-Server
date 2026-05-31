package team.carrypigeon.backend.chat.domain.features.channel.application.query;

/**
 * 远端频道发现查询。
 */
public record DiscoverChannelsQuery(long accountId, String keyword, Long cursorChannelId, String type, int limit) {
}

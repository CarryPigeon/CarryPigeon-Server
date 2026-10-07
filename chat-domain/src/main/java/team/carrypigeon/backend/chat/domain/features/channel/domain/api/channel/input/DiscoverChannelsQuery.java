package team.carrypigeon.backend.chat.domain.features.channel.domain.api.channel.input;



/**
 * 远端频道发现查询。
 */
public record DiscoverChannelsQuery(long accountId, String keyword, Long cursorChannelId, int limit) {
}

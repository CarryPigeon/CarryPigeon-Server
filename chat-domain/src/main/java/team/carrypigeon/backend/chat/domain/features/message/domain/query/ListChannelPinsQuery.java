package team.carrypigeon.backend.chat.domain.features.message.domain.query;

/**
 * 查询频道置顶列表。
 */
public record ListChannelPinsQuery(long accountId, long channelId, Long cursorMessageId, int limit) {
}

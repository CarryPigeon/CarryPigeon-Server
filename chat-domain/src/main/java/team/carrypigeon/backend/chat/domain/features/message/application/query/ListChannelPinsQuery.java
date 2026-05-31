package team.carrypigeon.backend.chat.domain.features.message.application.query;

/**
 * 查询频道置顶列表。
 */
public record ListChannelPinsQuery(long accountId, long channelId, Long cursorMessageId, int limit) {
}

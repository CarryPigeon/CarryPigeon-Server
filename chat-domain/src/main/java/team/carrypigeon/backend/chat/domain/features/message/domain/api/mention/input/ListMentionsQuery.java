package team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input;

/**
 * 查询提及收件箱。
 */
public record ListMentionsQuery(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
}

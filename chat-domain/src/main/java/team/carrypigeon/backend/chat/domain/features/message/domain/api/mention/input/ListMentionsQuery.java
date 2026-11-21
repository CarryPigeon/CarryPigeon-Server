package team.carrypigeon.backend.chat.domain.features.message.domain.api.mention.input;

/**
 * 查询指定账号的提及。cursorMentionId 是排他上界，null 表示首页；limit 为 1 至 50。
 */
public record ListMentionsQuery(long accountId, Long cursorMentionId, int limit, boolean unreadOnly) {
}

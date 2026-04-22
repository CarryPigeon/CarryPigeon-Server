package team.carrypigeon.backend.chat.domain.features.message.application.query;

/**
 * 频道历史消息查询。
 * 职责：表达按频道使用 cursor 查询历史消息的最小输入。
 * 边界：当前阶段不支持复杂筛选条件。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param cursorMessageId 游标消息 ID，可为空
 * @param limit 查询条数
 */
public record GetChannelMessageHistoryQuery(long accountId, long channelId, Long cursorMessageId, int limit) {
}

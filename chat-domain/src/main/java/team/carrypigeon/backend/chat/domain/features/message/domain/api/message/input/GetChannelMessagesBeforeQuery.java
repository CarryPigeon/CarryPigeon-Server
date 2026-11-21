package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

/**
 * 频道更旧消息查询。
 * 职责：表达以指定消息为排他游标、继续向更旧方向读取一页消息的最小输入。
 * 边界：游标必填；不承载锚点上下文或搜索条件。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param cursorMessageId 排他游标消息 ID
 * @param limit 查询条数
 */
public record GetChannelMessagesBeforeQuery(
        long accountId,
        long channelId,
        long cursorMessageId,
        int limit
) {
}

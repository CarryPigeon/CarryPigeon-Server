package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

/**
 * 频道最新消息查询。
 * 职责：表达从频道最新位置向前读取一页消息的最小输入。
 * 边界：不承载锚点、游标或搜索条件。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param limit 查询条数
 */
public record GetLatestChannelMessagesQuery(
        long accountId,
        long channelId,
        int limit
) {
}

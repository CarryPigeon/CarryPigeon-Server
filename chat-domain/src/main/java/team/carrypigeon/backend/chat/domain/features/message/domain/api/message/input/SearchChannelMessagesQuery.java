package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

/**
 * 频道消息搜索查询。
 * 职责：表达当前登录成员在指定频道内按关键字搜索消息的最小输入。
 * 边界：只承载搜索过滤条件与排他游标，不承载时间线方向或锚点上下文。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param keyword 搜索关键字
 * @param cursorMessageId 搜索排他游标消息 ID，可为空
 * @param senderAccountId 发送者账户 ID，可为空
 * @param domain 消息 domain，可为空
 * @param limit 返回条数
 */
public record SearchChannelMessagesQuery(
        long accountId,
        long channelId,
        String keyword,
        Long cursorMessageId,
        Long senderAccountId,
        String domain,
        int limit
) {
}

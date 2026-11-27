package team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input;

/**
 * 频道消息锚点上下文查询。
 * 职责：表达读取指定锚点消息前后固定窗口的最小输入。
 * 边界：不使用游标；窗口大小只表达数量，不表达翻页方向。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param anchorMessageId 锚点消息 ID
 * @param before 锚点之前数量
 * @param after 锚点之后数量
 */
public record GetChannelMessagesAroundQuery(
        long accountId,
        long channelId,
        long anchorMessageId,
        int before,
        int after
) {
}

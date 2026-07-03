package team.carrypigeon.backend.chat.domain.features.message.domain.query;

/**
 * 频道消息搜索查询。
 * 职责：表达当前登录成员在指定频道内按关键字搜索消息的最小输入。
 * 边界：当前阶段只覆盖最小关键字搜索，不展开复杂排序、高亮或跨频道聚合能力。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param keyword 搜索关键字
 * @param cursorMessageId 搜索游标消息 ID
 * @param senderAccountId 发送者账户 ID
 * @param domain 消息 domain
 * @param beforeMessageId 只查该消息之前的结果
 * @param afterMessageId 只查该消息之后的结果
 * @param limit 返回条数
 */
public record SearchChannelMessagesQuery(
        long accountId,
        long channelId,
        String keyword,
        Long cursorMessageId,
        Long senderAccountId,
        String domain,
        Long beforeMessageId,
        Long afterMessageId,
        int limit
) {
}

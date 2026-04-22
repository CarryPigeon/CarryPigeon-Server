package team.carrypigeon.backend.chat.domain.features.message.application.query;

/**
 * 频道消息搜索查询。
 * 职责：表达当前登录成员在指定频道内按关键字搜索消息的最小输入。
 * 边界：当前阶段只覆盖最小关键字搜索，不展开复杂排序、高亮或跨频道聚合能力。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 * @param keyword 搜索关键字
 * @param limit 返回条数
 */
public record SearchChannelMessagesQuery(long accountId, long channelId, String keyword, int limit) {
}

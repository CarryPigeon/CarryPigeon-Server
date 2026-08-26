package team.carrypigeon.backend.chat.domain.features.channel.domain.query;

/**
 * 频道置顶引用列表查询。
 * 职责：承载频道成员读取置顶记录所需的成员、游标和数量约束。
 *
 * @param accountId 查询账号 ID
 * @param channelId 频道 ID
 * @param cursorMessageId 可选消息游标
 * @param limit 最大返回数量
 */
public record ListChannelPinReferencesQuery(
        long accountId,
        long channelId,
        Long cursorMessageId,
        int limit
) {
}

package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 频道已读状态领域模型。
 * @param channelId 频道 ID
 * @param accountId 账号 ID
 * @param lastReadMessageId 最后已读消息的稳定身份
 * @param lastReadSequence 最后已读频道序号，推进比较与未读统计以此为准
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelReadState(
        long channelId,
        long accountId,
        long lastReadMessageId,
        long lastReadSequence,
        Instant createdAt,
        Instant updatedAt
) {
}

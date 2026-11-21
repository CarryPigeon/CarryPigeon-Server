package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 频道已读状态领域模型。
 * @param channelId 频道 ID
 * @param accountId 账号 ID
 * @param lastReadMessageId 已读边界；入群时频道为空则为 null
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelReadState(
        long channelId,
        long accountId,
        Long lastReadMessageId,
        Instant createdAt,
        Instant updatedAt
) {
}

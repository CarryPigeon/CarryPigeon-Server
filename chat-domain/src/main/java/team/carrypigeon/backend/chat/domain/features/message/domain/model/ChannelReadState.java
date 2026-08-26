package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 频道已读状态领域模型。
 */
public record ChannelReadState(
        long channelId,
        long accountId,
        long lastReadMessageId,
        Instant lastReadTime,
        Instant createdAt,
        Instant updatedAt
) {
}

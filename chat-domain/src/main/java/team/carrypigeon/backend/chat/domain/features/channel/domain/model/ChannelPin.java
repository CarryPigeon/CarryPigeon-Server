package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道置顶领域模型。
 */
public record ChannelPin(
        long pinId,
        long channelId,
        long messageId,
        long pinnedByAccountId,
        String note,
        Instant pinnedAt
) {
}

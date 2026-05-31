package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道置顶数据库记录契约。
 */
public record ChannelPinRecord(
        long pinId,
        long channelId,
        long messageId,
        long pinnedByAccountId,
        String note,
        Instant pinnedAt
) {
}

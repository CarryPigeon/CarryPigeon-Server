package team.carrypigeon.backend.chat.domain.features.message.application.dto;

import java.time.Instant;

/**
 * 频道置顶结果。
 */
public record ChannelPinResult(long pinId, long channelId, long messageId, long pinnedByAccountId, Instant pinnedAt, String note) {
}

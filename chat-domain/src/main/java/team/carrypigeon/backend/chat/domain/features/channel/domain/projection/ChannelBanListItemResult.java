package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

import java.time.Instant;

/**
 * 频道封禁列表项结果。
 */
public record ChannelBanListItemResult(long channelId, long bannedAccountId, Instant expiresAt, String reason, Instant createdAt) {
}

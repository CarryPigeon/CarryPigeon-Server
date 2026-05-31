package team.carrypigeon.backend.chat.domain.features.server.domain.model;

import java.time.Instant;

/**
 * 频道级通知偏好领域模型。
 */
public record NotificationChannelPreference(
        long accountId,
        long channelId,
        String mode,
        long mutedUntil,
        Instant createdAt,
        Instant updatedAt
) {
}

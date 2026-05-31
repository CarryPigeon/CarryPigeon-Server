package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道级通知偏好数据库记录契约。
 */
public record NotificationChannelPreferenceRecord(
        long accountId,
        long channelId,
        String mode,
        long mutedUntil,
        Instant createdAt,
        Instant updatedAt
) {
}

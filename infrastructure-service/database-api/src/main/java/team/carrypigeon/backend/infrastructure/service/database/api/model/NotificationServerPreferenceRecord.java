package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 服务端级通知偏好数据库记录契约。
 */
public record NotificationServerPreferenceRecord(
        long accountId,
        String mode,
        long mutedUntil,
        Instant createdAt,
        Instant updatedAt
) {
}

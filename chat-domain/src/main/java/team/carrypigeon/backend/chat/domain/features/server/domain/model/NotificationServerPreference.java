package team.carrypigeon.backend.chat.domain.features.server.domain.model;

import java.time.Instant;

/**
 * 服务端级通知偏好领域模型。
 */
public record NotificationServerPreference(
        long accountId,
        String mode,
        long mutedUntil,
        Instant createdAt,
        Instant updatedAt
) {
}

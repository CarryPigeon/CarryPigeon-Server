package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 提及数据库记录契约。
 */
public record MentionRecord(
        long mentionId,
        long channelId,
        long messageId,
        long fromAccountId,
        String targetType,
        long targetAccountId,
        Instant createdAt,
        boolean read
) {
}

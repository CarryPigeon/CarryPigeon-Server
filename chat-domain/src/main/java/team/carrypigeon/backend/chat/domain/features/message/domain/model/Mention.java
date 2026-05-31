package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 提及领域模型。
 */
public record Mention(
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

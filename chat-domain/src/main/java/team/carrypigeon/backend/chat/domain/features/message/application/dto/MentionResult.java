package team.carrypigeon.backend.chat.domain.features.message.application.dto;

import java.time.Instant;

/**
 * 提及结果。
 */
public record MentionResult(long mentionId, long channelId, long messageId, long fromAccountId, String targetType, long targetAccountId, Instant createdAt, boolean read) {
}

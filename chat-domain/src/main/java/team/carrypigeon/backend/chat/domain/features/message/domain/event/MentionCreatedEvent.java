package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.time.Instant;

/**
 * 用户消息提醒已创建事件。
 * 职责：表达提醒索引持久化后的稳定标识和参与账号。
 */
public record MentionCreatedEvent(
        long mentionId,
        long channelId,
        long messageId,
        long fromAccountId,
        long targetAccountId,
        Instant createdAt
) {
}

package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 提及数据库记录契约。
 * 职责：表达 chat_mention 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载提及解析规则。
 *
 * @param mentionId 提及 ID
 * @param channelId 频道 ID
 * @param messageId 来源消息 ID
 * @param fromAccountId 触发提及的账户 ID
 * @param targetType 被提及目标类型
 * @param targetAccountId 被提及账户 ID
 * @param createdAt 创建时间
 * @param read 是否已读
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

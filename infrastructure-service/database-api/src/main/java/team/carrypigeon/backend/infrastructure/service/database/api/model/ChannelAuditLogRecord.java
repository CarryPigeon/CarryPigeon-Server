package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道审计日志数据库记录契约。
 * 职责：在 chat-domain 与 database-impl 之间传递频道审计字段。
 * 边界：这里只表达数据库服务契约，不承载审计业务规则。
 *
 * @param auditId 审计记录 ID
 * @param channelId 频道 ID
 * @param actorAccountId 操作人账户 ID；系统触发时允许为空
 * @param actionType 操作类型
 * @param targetAccountId 目标账户 ID；无目标账户时允许为空
 * @param metadata 扩展元数据
 * @param createdAt 创建时间
 */
public record ChannelAuditLogRecord(
        long auditId,
        long channelId,
        Long actorAccountId,
        String actionType,
        Long targetAccountId,
        String metadata,
        Instant createdAt
) {
}

package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道审计日志写侧数据库记录契约。
 * 职责：表达频道审计日志写入时所需的最小持久化投影字段。
 * 边界：只服务数据库写侧契约，不承载审计查询语义。
 *
 * @param auditId 审计记录 ID
 * @param channelId 频道 ID
 * @param actorAccountId 操作人账户 ID；系统触发时允许为空
 * @param actionType 操作类型
 * @param targetAccountId 目标账户 ID；无目标账户时允许为空
 * @param metadata 扩展元数据
 * @param createdAt 创建时间
 */
public record ChannelAuditLogWriteRecord(
        long auditId,
        long channelId,
        Long actorAccountId,
        String actionType,
        Long targetAccountId,
        String metadata,
        Instant createdAt
) {
}

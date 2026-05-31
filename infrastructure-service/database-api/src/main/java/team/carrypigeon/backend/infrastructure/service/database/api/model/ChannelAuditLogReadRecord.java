package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道审计日志读侧数据库记录契约。
 */
public record ChannelAuditLogReadRecord(
        long auditId,
        long channelId,
        Long actorAccountId,
        String actionType,
        String metadata,
        Instant createdAt
) {
}

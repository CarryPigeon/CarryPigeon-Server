package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

/**
 * 审计日志结果。
 */
public record AuditLogResult(
        String auditId,
        String cid,
        String actorUid,
        String action,
        String details,
        long createdAt
) {
}

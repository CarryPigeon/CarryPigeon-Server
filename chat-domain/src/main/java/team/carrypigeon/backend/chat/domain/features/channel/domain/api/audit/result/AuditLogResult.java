package team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.result;


import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;

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

    public static AuditLogResult from(ChannelAuditLog log, String action) {
        return new AuditLogResult(
                IdUtil.toString(log.auditId()),
                IdUtil.toString(log.channelId()),
                log.actorAccountId() == null ? null : IdUtil.toString(log.actorAccountId()),
                action,
                log.metadata(),
                log.createdAt().toEpochMilli()
        );
    }
}

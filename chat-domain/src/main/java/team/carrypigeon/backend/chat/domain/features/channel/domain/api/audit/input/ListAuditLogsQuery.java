package team.carrypigeon.backend.chat.domain.features.channel.domain.api.audit.input;



/**
 * 审计日志列表查询。
 */
public record ListAuditLogsQuery(
        long accountId,
        Long cursorAuditId,
        int limit,
        Long channelId,
        Long actorAccountId,
        String action,
        Long fromTime,
        Long toTime
) {
}

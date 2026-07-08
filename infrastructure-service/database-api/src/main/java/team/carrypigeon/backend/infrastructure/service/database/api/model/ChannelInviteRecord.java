package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道邀请数据库记录契约。
 * 职责：表达频道邀请读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载额外邀请业务语义。
 *
 * @param channelId 频道 ID
 * @param applicationId 申请/邀请 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param reason 申请理由；普通邀请为空
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间；未响应时为空
 */
public record ChannelInviteRecord(
        long channelId,
        long applicationId,
        long inviteeAccountId,
        long inviterAccountId,
        String reason,
        String status,
        Instant createdAt,
        Instant respondedAt
) {

    public ChannelInviteRecord(
            long channelId,
            long applicationId,
            long inviteeAccountId,
            long inviterAccountId,
            String status,
            Instant createdAt,
            Instant respondedAt
    ) {
        this(channelId, applicationId, inviteeAccountId, inviterAccountId, null, status, createdAt, respondedAt);
    }
}

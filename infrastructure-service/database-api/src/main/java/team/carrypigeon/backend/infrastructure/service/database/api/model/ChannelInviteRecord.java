package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道邀请数据库记录契约。
 * 职责：在 chat-domain 与 database-impl 之间传递频道邀请字段。
 * 边界：这里只表达数据库服务契约，不承载邀请业务规则。
 *
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间；未响应时为空
 */
public record ChannelInviteRecord(
        long channelId,
        long inviteeAccountId,
        long inviterAccountId,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}

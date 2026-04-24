package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import java.time.Instant;

/**
 * 频道邀请响应。
 * 职责：向 HTTP 协议层返回邀请记录的稳定字段。
 * 边界：只承载协议输出，不承载邀请业务逻辑。
 *
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间
 */
public record ChannelInviteResponse(
        long channelId,
        long inviteeAccountId,
        long inviterAccountId,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}

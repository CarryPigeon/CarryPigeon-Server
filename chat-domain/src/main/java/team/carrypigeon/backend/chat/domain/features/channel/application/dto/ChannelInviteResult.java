package team.carrypigeon.backend.chat.domain.features.channel.application.dto;

import java.time.Instant;

/**
 * 频道邀请结果。
 * 职责：向协议层暴露邀请记录在当前切片内的稳定字段。
 * 边界：只承载应用层返回数据，不承载邀请状态流转逻辑。
 *
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间
 */
public record ChannelInviteResult(
        long channelId,
        long inviteeAccountId,
        long inviterAccountId,
        String status,
        Instant createdAt,
        Instant respondedAt
) {
}

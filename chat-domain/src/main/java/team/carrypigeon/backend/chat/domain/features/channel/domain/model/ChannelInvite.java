package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道邀请领域模型。
 * 职责：表达频道对目标账号的独立邀请记录。
 * 边界：邀请记录不等同于活跃成员，接受前不会进入 ChannelMember 投影。
 *
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 * @param inviterAccountId 发起邀请账户 ID
 * @param status 邀请状态
 * @param createdAt 创建时间
 * @param respondedAt 响应时间；未响应时为空
 */
public record ChannelInvite(
        long channelId,
        long inviteeAccountId,
        long inviterAccountId,
        ChannelInviteStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}

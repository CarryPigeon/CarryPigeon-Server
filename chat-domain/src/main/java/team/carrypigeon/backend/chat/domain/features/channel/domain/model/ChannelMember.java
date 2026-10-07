package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道成员领域模型。
 * 职责：表达活跃成员投影与频道治理直接依赖的固定角色。
 * 边界：这里只表示已加入频道的活跃成员；邀请与禁言由独立持久化模型表达。
 *
 * @param channelId 频道 ID
 * @param accountId 账户 ID
 * @param role 活跃成员固定角色
 * @param joinedAt 加入时间
 */
public record ChannelMember(
        long channelId,
        long accountId,
        ChannelMemberRole role,
        java.time.Instant joinedAt
) {
}

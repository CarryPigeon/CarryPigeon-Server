package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道邀请状态。
 * 职责：表达独立邀请记录在频道治理中的最小生命周期。
 * 边界：这里只定义邀请记录状态，不直接驱动成员表状态机。
 */
public enum ChannelInviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REVOKED
}

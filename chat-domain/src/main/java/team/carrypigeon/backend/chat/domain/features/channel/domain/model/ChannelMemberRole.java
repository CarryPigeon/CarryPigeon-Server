package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

/**
 * 频道成员固定角色。
 * 职责：为频道治理提供稳定且有限的角色语义。
 * 边界：只定义当前已确认的 OWNER / ADMIN / MEMBER，不扩展为通用权限矩阵。
 */
public enum ChannelMemberRole {
    OWNER,
    ADMIN,
    MEMBER
}

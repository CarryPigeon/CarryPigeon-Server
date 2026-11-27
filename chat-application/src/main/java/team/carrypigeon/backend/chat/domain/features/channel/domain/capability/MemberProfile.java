package team.carrypigeon.backend.chat.domain.features.channel.domain.capability;

/**
 * 频道成员展示资料。
 * 职责：承载频道视图需要的成员身份字段，作为跨 feature 资料的域内投影。
 * 边界：只含展示字段，不含账号凭据、权限或资料维护能力。
 *
 * @param accountId 成员账户 ID
 * @param username 成员用户名，同时作为公开昵称
 * @param avatarUrl 成员头像相对路径
 */
public record MemberProfile(long accountId, String username, String avatarUrl) {
}

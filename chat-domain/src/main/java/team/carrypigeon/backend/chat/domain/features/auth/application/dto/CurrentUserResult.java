package team.carrypigeon.backend.chat.domain.features.auth.application.dto;

/**
 * 当前用户结果。
 * 职责：返回 access token 已认证主体的最小账户信息。
 * 边界：不包含角色、权限或会话列表。
 *
 * @param accountId 当前账户 ID
 * @param username 当前用户名
 */
public record CurrentUserResult(long accountId, String username) {
}

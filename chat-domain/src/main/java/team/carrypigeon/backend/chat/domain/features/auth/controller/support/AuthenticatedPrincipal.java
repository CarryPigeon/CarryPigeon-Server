package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

/**
 * 当前请求认证主体。
 * 职责：表达 access token 校验后绑定到当前 HTTP 请求的最小身份信息。
 * 边界：不包含角色、权限或跨服务端身份语义。
 *
 * @param accountId 账户 ID
 * @param username 用户名
 */
public record AuthenticatedPrincipal(long accountId, String username) {
}

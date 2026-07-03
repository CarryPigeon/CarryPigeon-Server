package team.carrypigeon.backend.chat.domain.shared.domain.auth;

/**
 * 已认证账户快照。
 * 职责：作为 chat-domain 内跨 feature 复用的最小认证主体载荷。
 * 边界：只承载稳定账户标识与展示名，不携带 token、请求对象或协议细节。
 *
 * @param accountId 当前已认证账户 ID
 * @param username 当前已认证账户用户名
 */
public record AuthenticatedAccount(long accountId, String username) {
}

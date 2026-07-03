package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

/**
 * 旧 HTTP 认证主体兼容壳。
 * 职责：保留历史类型名，避免外围测试和装配代码在迁移期间断裂。
 * 边界：新的共享认证契约位于 shared 包，此类型仅用于兼容。
 *
 * @param accountId 账户 ID
 * @param username 用户名
 */
@Deprecated(forRemoval = false)
public record AuthenticatedPrincipal(long accountId, String username) {

    /**
     * 从共享认证主体创建兼容对象。
     *
     * @param authenticatedAccount 共享认证主体
     * @return 兼容主体
     */
    public static AuthenticatedPrincipal from(AuthenticatedAccount authenticatedAccount) {
        return new AuthenticatedPrincipal(authenticatedAccount.accountId(), authenticatedAccount.username());
    }

    /**
     * 转换为共享认证主体。
     *
     * @return 共享认证主体
     */
    public AuthenticatedAccount toAuthenticatedAccount() {
        return new AuthenticatedAccount(accountId, username);
    }
}

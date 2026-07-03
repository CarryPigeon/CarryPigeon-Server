package team.carrypigeon.backend.chat.domain.shared.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * HTTP 认证请求上下文。
 * 职责：在当前请求范围内读写已认证账户快照，供多个 feature 的控制器共享。
 * 边界：这里只负责请求级上下文传递，不负责 token 校验与会话持久化。
 */
@Component
public class RequestAuthenticationContext {

    private static final String AUTHENTICATED_ACCOUNT_ATTRIBUTE =
            RequestAuthenticationContext.class.getName() + ".authenticatedAccount";

    /**
     * 将已认证账户绑定到当前 HTTP 请求。
     *
     * @param request 当前 HTTP 请求
     * @param authenticatedAccount 已认证账户快照
     */
    public void bind(HttpServletRequest request, AuthenticatedAccount authenticatedAccount) {
        request.setAttribute(AUTHENTICATED_ACCOUNT_ATTRIBUTE, authenticatedAccount);
    }

    /**
     * 读取当前请求的已认证账户。
     *
     * @param request 当前 HTTP 请求
     * @return 已认证账户快照
     */
    public AuthenticatedAccount requirePrincipal(HttpServletRequest request) {
        Object authenticatedAccount = request.getAttribute(AUTHENTICATED_ACCOUNT_ATTRIBUTE);
        if (authenticatedAccount instanceof AuthenticatedAccount account) {
            return account;
        }
        throw ProblemException.forbidden("authentication_required", "authentication is required");
    }
}

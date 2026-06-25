package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;

/**
 * 旧鉴权请求上下文兼容壳。
 * 职责：为历史测试和装配代码保留旧类型名，并转发到共享请求认证上下文。
 * 边界：不再作为主入口注入，新的跨 feature 代码应直接依赖 shared 包中的类型。
 */
@Deprecated(forRemoval = false)
public class AuthRequestContext extends RequestAuthenticationContext {

    /**
     * 兼容旧调用方传入历史主体类型。
     *
     * @param request 当前 HTTP 请求
     * @param principal 历史认证主体
     */
    public void bind(HttpServletRequest request, AuthenticatedPrincipal principal) {
        super.bind(request, principal.toAuthenticatedAccount());
    }
}

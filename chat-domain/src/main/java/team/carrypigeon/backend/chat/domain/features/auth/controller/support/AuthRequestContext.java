package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * HTTP 鉴权请求上下文。
 * 职责：在 Spring MVC 请求范围内读写已认证主体。
 * 边界：不使用全局静态会话，不承载 token 校验逻辑。
 */
@Component
public class AuthRequestContext {

    private static final String PRINCIPAL_ATTRIBUTE = AuthRequestContext.class.getName() + ".principal";

    /**
     * 将认证主体绑定到请求。
     *
     * @param request 当前 HTTP 请求
     * @param principal 认证主体
     */
    public void bind(HttpServletRequest request, AuthenticatedPrincipal principal) {
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
    }

    /**
     * 读取当前请求认证主体。
     *
     * @param request 当前 HTTP 请求
     * @return 认证主体
     */
    public AuthenticatedPrincipal requirePrincipal(HttpServletRequest request) {
        Object principal = request.getAttribute(PRINCIPAL_ATTRIBUTE);
        if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
            return authenticatedPrincipal;
        }
        throw ProblemException.forbidden("authentication_required", "authentication is required");
    }
}

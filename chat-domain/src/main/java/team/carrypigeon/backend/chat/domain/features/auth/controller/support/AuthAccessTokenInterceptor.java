package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

/**
 * Access token HTTP 拦截器。
 * 职责：对受保护 HTTP API 校验 Bearer access token 并绑定当前请求身份。
 * 边界：不处理角色权限，不处理 refresh token 与会话撤销。
 */
public class AuthAccessTokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService authTokenService;
    private final AuthRequestContext authRequestContext;

    public AuthAccessTokenInterceptor(AuthTokenService authTokenService, AuthRequestContext authRequestContext) {
        this.authTokenService = authTokenService;
        this.authRequestContext = authRequestContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw ProblemException.forbidden("authentication_required", "authentication is required");
        }

        AuthTokenClaims claims = authTokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(Long.parseLong(claims.subject()), claims.username());
        authRequestContext.bind(request, principal);
        LogContexts.uid(Long.toString(principal.accountId()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        LogContexts.remove(LogKeys.UID);
    }
}

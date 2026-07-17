package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
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
    private final RequestAuthenticationContext authRequestContext;

    public AuthAccessTokenInterceptor(AuthTokenService authTokenService, RequestAuthenticationContext authRequestContext) {
        this.authTokenService = authTokenService;
        this.authRequestContext = authRequestContext;
    }

    /**
     * 校验请求头中的 Bearer access token 并绑定当前主体。
     * 输入：HTTP 请求上下文。
     * 副作用：向请求上下文和日志上下文写入当前账号身份。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前处理器
     * @return 认证成功时始终返回 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw ProblemException.forbidden("authentication_required", "authentication is required");
        }

        AuthTokenClaims claims = authTokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
        AuthenticatedAccount authenticatedAccount = new AuthenticatedAccount(parseAccountId(claims), claims.username());
        authRequestContext.bind(request, authenticatedAccount);
        LogContexts.uid(Long.toString(authenticatedAccount.accountId()));
        return true;
    }

    /**
     * 从 access token claims 中解析当前账号 ID。
     * 失败语义：token subject 不是正数账号 ID 时返回非法 access token。
     *
     * @param claims 已校验的 access token claims
     * @return 当前账号 ID
     */
    private long parseAccountId(AuthTokenClaims claims) {
        try {
            long accountId = Long.parseLong(claims.subject());
            if (accountId <= 0L) {
                throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
            }
            return accountId;
        } catch (NumberFormatException exception) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
    }

    /**
     * 清理请求级日志上下文。
     * 副作用：移除当前线程中的账号日志键，避免污染后续请求。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前处理器
     * @param exception 请求处理过程中抛出的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        LogContexts.remove(LogKeys.UID);
    }
}

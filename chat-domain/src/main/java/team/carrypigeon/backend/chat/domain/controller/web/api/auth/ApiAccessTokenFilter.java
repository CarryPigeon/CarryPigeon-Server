package team.carrypigeon.backend.chat.domain.controller.web.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorBody;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * API Access Token 认证过滤器。
 * <p>
 * 负责从 `Authorization` 头提取 Bearer Token，校验通过后写入请求用户上下文。
 */
@Component
public class ApiAccessTokenFilter extends OncePerRequestFilter {

    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    /**
     * 构造认证过滤器。
     *
     * @param accessTokenService Access Token 服务。
     * @param objectMapper JSON 序列化组件。
     */
    public ApiAccessTokenFilter(AccessTokenService accessTokenService, ObjectMapper objectMapper) {
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 判断当前请求是否跳过鉴权过滤。
     *
     * @param request HTTP 请求对象。
     * @return 需要跳过过滤返回 {@code true}，否则返回 {@code false}。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if (!path.startsWith("/api")) {
            return true;
        }
        return path.equals("/api/server")
                || path.equals("/api/gates/required/check")
                || path.equals("/api/plugins/catalog")
                || path.equals("/api/domains/catalog")
                || path.startsWith("/api/plugins/download/")
                || path.startsWith("/api/contracts/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/ws")
                || path.startsWith("/api/files/download/");
    }

    /**
     * 执行 Access Token 鉴权。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @param filterChain 过滤器链。
     * @throws ServletException Servlet 过滤异常。
     * @throws IOException IO 异常。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = extractBearer(auth);
        Long uid = accessTokenService.verify(token);
        if (uid == null) {
            writeUnauthorized(request, response);
            return;
        }
        request.setAttribute(ApiAuth.REQ_ATTR_UID, uid);
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头提取 Bearer Token。
     *
     * @param auth Authorization 请求头值。
     * @return Bearer Token；格式不合法时返回 {@code null}。
     */
    private String extractBearer(String auth) {
        if (auth == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }
        return auth.substring(prefix.length()).trim();
    }

    /**
     * 输出 401 未授权响应。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @throws IOException 当响应输出失败时抛出。
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = (String) request.getAttribute("cp_request_id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
            request.setAttribute("cp_request_id", requestId);
        }
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorBody body = new ApiErrorBody(401, "unauthorized", "missing or invalid access token", requestId, null);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(body));
    }
}

package team.carrypigeon.backend.chat.domain.controller.web.api.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorBody;
import team.carrypigeon.backend.chat.domain.controller.web.api.error.ApiErrorResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP `/api` 版本协商过滤器。
 * <p>
 * 负责解析 `Accept` 头中的版本参数，并对不支持版本返回标准错误响应。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final String MEDIA = "application/vnd.carrypigeon+json";
    private static final String EXPECTED_VERSION = "1";

    private final ObjectMapper objectMapper;

    /**
     * 构造版本过滤器。
     *
     * @param objectMapper JSON 序列化组件。
     */
    public ApiVersionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 判断当前请求是否跳过版本过滤。
     *
     * @param request HTTP 请求对象。
     * @return 需要跳过过滤返回 {@code true}，否则返回 {@code false}。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api")) {
            return true;
        }
        return path.startsWith("/api/ws");
    }

    /**
     * 执行版本协商并在响应阶段回写标准媒体类型。
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
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MEDIA)) {
            String version = parseVersion(accept);
            if (version != null && !EXPECTED_VERSION.equals(version)) {
                writeUnsupported(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
        if (!response.isCommitted() && response.getContentType() != null
                && response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            response.setHeader(HttpHeaders.CONTENT_TYPE, MEDIA + "; version=" + EXPECTED_VERSION);
        }
    }

    /**
     * 从 Accept 头中解析版本号。
     *
     * @param accept Accept 请求头值。
     * @return 解析出的版本号；不存在时返回 {@code null}。
     */
    private String parseVersion(String accept) {
        int mediaPos = accept.indexOf(MEDIA);
        if (mediaPos < 0) {
            return null;
        }
        int vPos = accept.indexOf("version=", mediaPos);
        if (vPos < 0) {
            return null;
        }
        int start = vPos + "version=".length();
        int end = start;
        while (end < accept.length()) {
            char c = accept.charAt(end);
            if (c == ';' || c == ',' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        String v = accept.substring(start, end).trim();
        return v.isEmpty() ? null : v;
    }

    /**
     * 输出不支持版本错误。
     *
     * @param request HTTP 请求对象。
     * @param response HTTP 响应对象。
     * @throws IOException 当响应输出失败时抛出。
     */
    private void writeUnsupported(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = (String) request.getAttribute("cp_request_id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
            request.setAttribute("cp_request_id", requestId);
        }
        CPProblemReason reason = CPProblemReason.API_VERSION_UNSUPPORTED;
        response.setStatus(reason.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorBody body = new ApiErrorBody(reason.status(), reason.code(), "api version unsupported", requestId, null);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(body));
    }
}

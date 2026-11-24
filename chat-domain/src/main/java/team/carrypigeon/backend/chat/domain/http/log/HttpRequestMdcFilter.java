package team.carrypigeon.backend.chat.domain.http.log;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogScope;

/**
 * HTTP 请求 MDC 上下文过滤器。
 * 职责：为每个 HTTP 请求写入最小日志上下文字段，并在请求结束后统一清理。
 * 边界：这里只处理协议层日志上下文，不承载认证、鉴权或业务决策逻辑。
 */
public class HttpRequestMdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        String route = resolveRoute(request);
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.HTTP)) {
            response.setHeader(REQUEST_ID_HEADER, requestId);
            LogContexts.sessionId(requestId);
            LogContexts.traceId(traceId);
            LogContexts.route(route);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 解析或生成请求 ID。
     * 语义：客户端未提供 `X-Request-Id` 时生成服务端请求 ID，保证每次请求都可关联日志。
     *
     * @param request HTTP 请求
     * @return 请求 ID
     */
    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    /**
     * 解析链路追踪 ID。
     * 语义：客户端未提供 `X-Trace-Id` 时复用 requestId，保证单请求链路至少有稳定 trace 标识。
     *
     * @param request   HTTP 请求
     * @param requestId 已解析的请求 ID
     * @return trace ID
     */
    private String resolveTraceId(HttpServletRequest request, String requestId) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return traceId == null || traceId.isBlank() ? requestId : traceId;
    }

    /**
     * 解析请求的路径
     *
     * @param request HTTP 请求
     * @return 请求url
     *
     */
    private String resolveRoute(HttpServletRequest request) {
        return request.getRequestURI();
    }
}

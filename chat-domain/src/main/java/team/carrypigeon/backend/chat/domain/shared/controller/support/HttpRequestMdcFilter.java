package team.carrypigeon.backend.chat.domain.shared.controller.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * HTTP 请求 MDC 上下文过滤器。
 * 职责：为每个 HTTP 请求写入最小日志上下文字段，并在请求结束后统一清理。
 * 边界：这里只处理协议层日志上下文，不承载认证、鉴权或业务决策逻辑。
 */
@Component
public class HttpRequestMdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        try {
            LogContexts.requestId(requestId);
            LogContexts.traceId(traceId);
            LogContexts.route(resolveRoute(request));
            filterChain.doFilter(request, response);
        } finally {
            LogContexts.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private String resolveTraceId(HttpServletRequest request, String requestId) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return traceId == null || traceId.isBlank() ? requestId : traceId;
    }

    private String resolveRoute(HttpServletRequest request) {
        return request.getRequestURI();
    }
}

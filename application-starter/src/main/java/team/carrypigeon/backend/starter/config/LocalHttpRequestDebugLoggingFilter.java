package team.carrypigeon.backend.starter.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 本地 HTTP 请求调试日志过滤器。
 * 职责：在本地联调期开关开启后输出每个 HTTP 请求的最小诊断摘要。
 * 边界：不读取请求体或响应体，不记录敏感认证头，只记录路由、来源、状态与耗时。
 */
@Slf4j
public class LocalHttpRequestDebugLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Exception failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info(buildRequestSummary(request, response, durationMs, failure));
        }
    }

    String buildRequestSummary(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs,
            Exception failure
    ) {
        return "Action: local_http_request_completed"
                + " method=" + request.getMethod()
                + " uri=" + request.getRequestURI()
                + " query=" + sanitizeQuery(request.getQueryString())
                + " origin=" + safeHeader(request, "Origin")
                + " accessControlRequestMethod=" + safeHeader(request, "Access-Control-Request-Method")
                + " status=" + response.getStatus()
                + " durationMs=" + durationMs
                + " remoteAddr=" + request.getRemoteAddr()
                + " userAgent=" + safeHeader(request, "User-Agent")
                + " failure=" + (failure == null ? "" : failure.getClass().getSimpleName());
    }

    String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? "" : value;
    }

    String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String sanitized = query;
        sanitized = sanitized.replaceAll("(?i)(access_token|refresh_token|token|password|secret|code)=([^&]*)", "$1=***");
        return sanitized;
    }
}

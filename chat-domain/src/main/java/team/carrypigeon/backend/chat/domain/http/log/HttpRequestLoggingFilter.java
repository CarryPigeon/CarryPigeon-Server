package team.carrypigeon.backend.chat.domain.http.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 在 Security 之外汇总一次 API 调用，读取响应写出阶段登记的业务码。
 */
@Slf4j
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    public static final String CODE = HttpRequestLoggingFilter.class.getName() + ".code";
    public static final String WRITE_FAILED = HttpRequestLoggingFilter.class.getName() + ".writeFailed";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        boolean returned = false;
        try {
            filterChain.doFilter(request, response);
            returned = true;
        } finally {
            boolean writeFailed = request.getAttribute(WRITE_FAILED) != null || (!returned && response.isCommitted());
            Integer code = (Integer) request.getAttribute(CODE);
            boolean failed = !returned || writeFailed || response.getStatus() >= 400 || (code != null && code != 0);
            var event = log.atInfo()
                .addKeyValue("action", "http.request")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("outcome", failed ? "failure" : "success")
                .addKeyValue("duration_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            if (returned || response.isCommitted()) {
                event.addKeyValue("http_status", response.getStatus());
            }
            if (writeFailed) {
                event.addKeyValue("stage", "response_write");
            } else if (returned && code != null) {
                event.addKeyValue("business_code", code);
            }
            event.log("HTTP 请求结束");
        }
    }
}

package team.carrypigeon.backend.starter.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 插件初始化期间的 HTTP 门禁。
 *
 * <p>职责：在 readiness gate 未打开时返回 503。边界：只保护 Servlet HTTP 请求，不替代负载均衡器
 * readiness，也不影响 JVM 启动前的安全模式 classpath 排除。</p>
 */
public final class PluginStartupGateFilter extends OncePerRequestFilter {

    private final PluginReadinessGate readinessGate;

    public PluginStartupGateFilter(PluginReadinessGate readinessGate) {
        this.readinessGate = readinessGate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (readinessGate.isReady()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":{\"status\":503,\"reason\":\"plugin_starting\"}}");
    }
}

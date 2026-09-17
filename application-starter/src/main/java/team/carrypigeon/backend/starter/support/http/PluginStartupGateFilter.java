package team.carrypigeon.backend.starter.support.http;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import team.carrypigeon.backend.starter.bootstrap.plugin.PluginReadinessGate;

/**
 * 插件初始化期间的 HTTP 门禁。
 * 职责：在 readiness gate 未打开时返回 503。
 * 边界：只适配 Servlet HTTP；独立 realtime 监听器由插件生命周期 phase 控制启动顺序。
 */
public final class PluginStartupGateFilter extends OncePerRequestFilter {

    private static final String READINESS_PATH = "/internal/readiness";

    private final PluginReadinessGate readinessGate;

    public PluginStartupGateFilter(PluginReadinessGate readinessGate) {
        this.readinessGate = readinessGate;
    }

    /**
     * readiness 入口必须直接读取门禁状态，避免被同一门禁提前拦截。
     *
     * @param request 当前 HTTP 请求
     * @return readiness 请求返回 true，其余请求返回 false
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return READINESS_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
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

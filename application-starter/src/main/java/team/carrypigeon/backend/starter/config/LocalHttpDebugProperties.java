package team.carrypigeon.backend.starter.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地 HTTP 调试配置。
 * 职责：承载本地客户端联调阶段需要的 CORS 与请求摘要日志开关。
 * 边界：只表达启动层运行时差异，不承载业务接口或鉴权规则。
 *
 * @param cors 本地 CORS 调试配置
 * @param requestLog HTTP 请求摘要日志配置
 */
@ConfigurationProperties(prefix = "cp.local-dev.http")
public record LocalHttpDebugProperties(
        Cors cors,
        RequestLog requestLog
) {

    private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://tauri.localhost",
            "https://tauri.localhost",
            "tauri://localhost"
    );
    private static final List<String> DEFAULT_ALLOWED_METHODS = List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    );
    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("*");
    private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of("X-Request-Id", "X-Trace-Id");
    private static final long DEFAULT_MAX_AGE_SECONDS = 3600L;

    public LocalHttpDebugProperties {
        if (cors == null) {
            cors = new Cors(false, null, null, null, null, null);
        }
        if (requestLog == null) {
            requestLog = new RequestLog(false);
        }
    }

    /**
     * 本地开发 CORS 配置。
     * 职责：为本地 WebView / Tauri / 浏览器调试入口提供受限跨域白名单。
     * 边界：只注册到 `/api/**`，不改变业务接口权限。
     *
     * @param enabled 是否启用本地 CORS
     * @param allowedOriginPatterns 允许的 Origin pattern
     * @param allowedMethods 允许的 HTTP 方法
     * @param allowedHeaders 允许的请求头
     * @param exposedHeaders 允许客户端读取的响应头
     * @param maxAge 预检请求缓存秒数
     */
    public record Cors(
            boolean enabled,
            List<String> allowedOriginPatterns,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            List<String> exposedHeaders,
            Long maxAge
    ) {

        public Cors {
            allowedOriginPatterns = normalizeList(allowedOriginPatterns, DEFAULT_ALLOWED_ORIGIN_PATTERNS);
            allowedMethods = normalizeList(allowedMethods, DEFAULT_ALLOWED_METHODS);
            allowedHeaders = normalizeList(allowedHeaders, DEFAULT_ALLOWED_HEADERS);
            exposedHeaders = normalizeList(exposedHeaders, DEFAULT_EXPOSED_HEADERS);
            maxAge = maxAge == null || maxAge < 0 ? DEFAULT_MAX_AGE_SECONDS : maxAge;
            if (enabled && allowedOriginPatterns.isEmpty()) {
                throw new IllegalArgumentException("cp.local-dev.http.cors.allowed-origin-patterns must not be empty when CORS is enabled");
            }
        }
    }

    /**
     * 本地 HTTP 请求摘要日志配置。
     * 职责：控制是否输出逐请求调试摘要。
     * 边界：不记录请求体、响应体、Cookie 或 Authorization。
     *
     * @param enabled 是否启用请求摘要日志
     */
    public record RequestLog(boolean enabled) {
    }

    private static List<String> normalizeList(List<String> raw, List<String> defaults) {
        if (raw == null || raw.isEmpty()) {
            return defaults;
        }
        List<String> normalized = raw.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        return normalized.isEmpty() ? defaults : normalized;
    }
}

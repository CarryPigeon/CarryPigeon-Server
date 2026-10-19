package team.carrypigeon.backend.chat.domain.config.ws;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径、线程模型、连接超时与事件窗口参数，并给出可公开的 websocket 地址。
 * 边界：只承载稳定运行参数与地址拼装，不承载启动逻辑、连接状态或业务语义。
 * 约束：不可变记录只能构造器绑定，因此由 `@ConfigurationPropertiesScan` 启用，而不是标 `@Component`。
 */
@ConfigurationProperties(prefix = "cp.chat.server.realtime")
public record RealtimeServerProperties(
        boolean enabled,
        String host,
        int port,
        String path,
        int bossThreads,
        int workerThreads,
        @DefaultValue("10")
        int authenticationTimeoutSeconds,
        @DefaultValue("60")
        int readIdleTimeoutSeconds,
        @DefaultValue("1h")
        Duration eventRetention,
        @DefaultValue("10000")
        int maxEventAccounts,
        @DefaultValue("1000")
        int maxEventsPerAccount,
        @DefaultValue("262144")
        int maxPayloadBytes
) {

    public RealtimeServerProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        if (bossThreads < 0) {
            throw new IllegalArgumentException("bossThreads must be greater than or equal to 0");
        }
        if (workerThreads < 0) {
            throw new IllegalArgumentException("workerThreads must be greater than or equal to 0");
        }
        if (authenticationTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("authenticationTimeoutSeconds must be greater than 0");
        }
        if (readIdleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("readIdleTimeoutSeconds must be greater than 0");
        }
        if (eventRetention == null || eventRetention.isZero() || eventRetention.isNegative()) {
            throw new IllegalArgumentException("eventRetention must be positive");
        }
        if (maxEventAccounts <= 0) {
            throw new IllegalArgumentException("maxEventAccounts must be greater than 0");
        }
        if (maxEventsPerAccount <= 0) {
            throw new IllegalArgumentException("maxEventsPerAccount must be greater than 0");
        }
        if (maxPayloadBytes <= 0) {
            throw new IllegalArgumentException("maxPayloadBytes must be greater than 0");
        }
    }

    /**
     * 返回服务发现可公开的 websocket 地址。
     *
     * @return 启用时返回 ws 地址；禁用时返回 null
     */
    public String wsUrl() {
        if (!enabled) {
            return null;
        }
        return "ws://" + host + ":" + port + path;
    }
}

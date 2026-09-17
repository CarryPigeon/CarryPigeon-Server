package team.carrypigeon.backend.chat.domain.features.server.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Netty 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径、线程模型和连接超时配置。
 * 边界：这里只承载稳定运行参数，不承载启动逻辑。
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
        int maxEventAccounts
) {

    @ConstructorBinding
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
    }

    public RealtimeServerProperties(
            boolean enabled,
            String host,
            int port,
            String path,
            int bossThreads,
            int workerThreads,
            int authenticationTimeoutSeconds,
            int readIdleTimeoutSeconds
    ) {
        this(
                enabled,
                host,
                port,
                path,
                bossThreads,
                workerThreads,
                authenticationTimeoutSeconds,
                readIdleTimeoutSeconds,
                Duration.ofHours(1),
                10_000
        );
    }

}

package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netty 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径和线程模型配置。
 * 边界：这里只承载稳定运行参数，不承载启动逻辑。
 */
@ConfigurationProperties(prefix = "cp.chat.server.realtime")
public record RealtimeServerProperties(
        boolean enabled,
        String host,
        int port,
        String path,
        int bossThreads,
        int workerThreads
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
    }

}

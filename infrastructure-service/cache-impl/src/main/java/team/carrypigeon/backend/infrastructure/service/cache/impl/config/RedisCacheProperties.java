package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Redis 连接、命令执行和缓存有效期配置；两个超时均须为正数，单位由 Duration 保留。 */
@ConfigurationProperties(prefix = "service.cache.redis")
public record RedisCacheProperties(
        boolean enabled,
        String host,
        int port,
        String password,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("3s") Duration timeout,
        Duration defaultTtl
) {
    public RedisCacheProperties {
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("service.cache.redis.connect-timeout must be positive");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("service.cache.redis.timeout must be positive");
        }
    }
}

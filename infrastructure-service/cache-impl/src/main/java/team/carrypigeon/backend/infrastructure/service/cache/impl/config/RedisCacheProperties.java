package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import java.time.Duration;
import team.carrypigeon.backend.infrastructure.basic.environment.InEnvironment;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Redis 连接、命令执行和缓存有效期配置；两个超时均须为正数，单位由 Duration 保留。 */
@InEnvironment(profiles = {PROD, DEV})
@ConfigurationProperties(prefix = "service.cache.redis")
public record RedisCacheProperties(
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

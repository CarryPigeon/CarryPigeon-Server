package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.cache.redis")
public record RedisCacheProperties(
        boolean enabled,
        String host,
        int port,
        String password,
        Duration connectTimeout,
        Duration timeout,
        Duration defaultTtl
) {}

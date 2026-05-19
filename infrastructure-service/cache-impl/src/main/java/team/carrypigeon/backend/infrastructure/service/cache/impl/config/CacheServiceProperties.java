package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存服务配置。
 * 职责：控制 cache-impl 是否装配以及默认缓存 TTL。
 * 边界：Redis 相关运行配置仍由 Spring 标准配置承接，这里只补充模块级开关和默认值。
 *
 * @param enabled 是否启用缓存服务实现
 * @param defaultTtl 默认缓存 TTL
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.cache")
public record CacheServiceProperties(boolean enabled, Duration defaultTtl) {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    public CacheServiceProperties {
        if (defaultTtl == null) {
            defaultTtl = DEFAULT_TTL;
        }
        if (defaultTtl.isZero() || defaultTtl.isNegative()) {
            throw new IllegalArgumentException("cp.infrastructure.service.cache.default-ttl must be positive");
        }
    }

    public CacheServiceProperties() {
        this(true, DEFAULT_TTL);
    }
}

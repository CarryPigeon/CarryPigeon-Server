package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证缓存实现配置的最小契约。
 * 职责：确保 cache-impl 的默认开关和默认 TTL 稳定可用。
 * 边界：不验证 Spring 绑定流程，只验证配置对象自身默认值。
 */
class CacheServicePropertiesTests {

    /**
     * 测试默认缓存配置。
     * 输入：不传入任何自定义配置。
     * 期望：启用缓存实现，并使用五分钟默认 TTL。
     */
    @Test
    void constructor_default_usesStableDefaults() {
        CacheServiceProperties properties = new CacheServiceProperties();

        assertEquals(true, properties.enabled());
        assertEquals(Duration.ofMinutes(5), properties.defaultTtl());
    }
}

package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证缓存实现配置的最小契约。
 * 职责：确保 cache-impl 的默认开关和默认 TTL 稳定可用。
 * 边界：不验证 Spring 绑定流程，只验证配置对象自身默认值。
 */
@Tag("unit")
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

    /**
     * 测试显式传入 null TTL 时回退为稳定默认值。
     * 输入：启用开关和 null 默认 TTL。
     * 期望：配置对象使用五分钟默认 TTL。
     */
    @Test
    void constructor_nullDefaultTtl_usesStableDefault() {
        CacheServiceProperties properties = new CacheServiceProperties(true, null);

        assertEquals(true, properties.enabled());
        assertEquals(Duration.ofMinutes(5), properties.defaultTtl());
    }

    /**
     * 测试非法 TTL 会在配置对象构造阶段被拒绝。
     * 输入：零或负数默认 TTL。
     * 期望：抛出可读的非法参数异常，避免错误配置被静默吞掉。
     */
    @Test
    void constructor_nonPositiveDefaultTtl_rejected() {
        IllegalArgumentException zeroException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheServiceProperties(true, Duration.ZERO)
        );
        IllegalArgumentException negativeException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheServiceProperties(true, Duration.ofSeconds(-1))
        );

        assertEquals("cp.infrastructure.service.cache.default-ttl must be positive", zeroException.getMessage());
        assertEquals("cp.infrastructure.service.cache.default-ttl must be positive", negativeException.getMessage());
    }
}

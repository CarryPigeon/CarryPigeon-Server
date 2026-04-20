package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证缓存自动配置的装配边界。
 * 职责：确保 cache-impl 只在启用条件满足时注册缓存服务相关 Bean。
 * 边界：不连接真实 Redis，只验证自动配置的上下文装配行为。
 */
class CacheServiceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheServiceAutoConfiguration.class));

    /**
     * 测试启用缓存服务时的自动配置。
     * 输入：启用开关、默认 TTL 和 Redis 字符串模板。
     * 期望：成功装配缓存服务与缓存健康检查服务。
     */
    @Test
    void autoConfiguration_enabled_registersCacheBeans() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.cache.enabled=true",
                        "cp.infrastructure.service.cache.default-ttl=5m"
                )
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheService.class);
                    assertThat(context).hasSingleBean(CacheHealthService.class);
                });
    }

    /**
     * 测试禁用缓存服务时的自动配置。
     * 输入：禁用开关和 Redis 字符串模板。
     * 期望：cache-impl 不注册缓存服务相关 Bean。
     */
    @Test
    void autoConfiguration_disabled_skipsCacheBeans() {
        contextRunner
                .withPropertyValues("cp.infrastructure.service.cache.enabled=false")
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheService.class);
                    assertThat(context).doesNotHaveBean(CacheHealthService.class);
                });
    }
}

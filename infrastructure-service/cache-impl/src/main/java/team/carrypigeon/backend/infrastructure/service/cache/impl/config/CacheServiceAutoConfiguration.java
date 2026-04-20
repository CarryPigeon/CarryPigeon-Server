package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.health.RedisCacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.redis.RedisCacheService;

/**
 * 缓存服务自动配置。
 * 职责：在 cache-impl 内装配 Redis 字符串缓存实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中声明 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(CacheServiceProperties.class)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheServiceAutoConfiguration {

    /**
     * 创建字符串缓存服务。
     *
     * @param redisTemplate Redis 字符串模板
     * @param properties 缓存服务配置
     * @return 字符串缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(StringRedisTemplate redisTemplate, CacheServiceProperties properties) {
        return new RedisCacheService(redisTemplate, properties);
    }

    /**
     * 创建缓存健康检查服务。
     *
     * @param redisTemplate Redis 字符串模板
     * @return 缓存健康检查服务
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheHealthService cacheHealthService(StringRedisTemplate redisTemplate) {
        return new RedisCacheHealthService(redisTemplate);
    }
}

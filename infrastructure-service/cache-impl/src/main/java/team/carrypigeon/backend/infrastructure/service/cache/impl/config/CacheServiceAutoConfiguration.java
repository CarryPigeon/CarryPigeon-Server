package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationChecker;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.health.RedisCacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.redis.RedisCacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.startup.CacheInitializationChecker;

/**
 * 缓存服务自动配置。
 * 职责：在 cache-impl 内装配 Redis 字符串缓存实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中声明 Spring 装配逻辑。
 */
@EnableCaching
@AutoConfiguration
@EnableConfigurationProperties(RedisCacheProperties.class)
@ConditionalOnProperty(prefix = "cp.service.cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory(RedisCacheProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.host());
        config.setPort(properties.port());
        if (properties.password() != null && !properties.password().isBlank()) {
            config.setPassword(properties.password());
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setTimeout(properties.timeout().toSeconds());
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(StringRedisTemplate redisTemplate, RedisCacheProperties properties) {
        return new RedisCacheService(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheHealthService cacheHealthService(StringRedisTemplate redisTemplate) {
        return new RedisCacheHealthService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cacheInitializationCheck")
    public InitializationChecker cacheInitializationCheck(CacheHealthService cacheHealthService) {
        return new CacheInitializationChecker(cacheHealthService);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.create(connectionFactory);
    }
}

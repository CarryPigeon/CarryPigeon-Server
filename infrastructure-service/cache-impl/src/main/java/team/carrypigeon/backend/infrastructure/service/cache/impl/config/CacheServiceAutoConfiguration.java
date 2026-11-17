package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.health.RedisCacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.redis.RedisCacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.redis.RedisCodeChallengeStore;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CodeChallengeStore;

/**
 * 缓存服务自动配置。
 * 职责：在 cache-impl 内装配 Redis 字符串缓存实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中声明 Spring 装配逻辑；数据库查询缓存由 database-impl 的
 * 自定义 key-value 门面实现，不依赖 Spring Cache 注解或 CacheManager。
 * 约束：邮箱验证码能力与数据库查询缓存都强依赖本实现，关闭前需确认相关业务组件具备替代实现。
 */
@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@EnableConfigurationProperties(RedisCacheProperties.class)
@ConditionalOnProperty(prefix = "service.cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
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
        // 使用 Duration 保留毫秒精度；连接建立和 Redis 命令执行分别采用各自的超时预算。
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(properties.timeout())
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder().connectTimeout(properties.connectTimeout()).build())
                        .build())
                .build();
        return new LettuceConnectionFactory(config, clientConfiguration);
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
    public CodeChallengeStore codeChallengeStore(StringRedisTemplate redisTemplate) {
        return new RedisCodeChallengeStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheHealthService cacheHealthService(StringRedisTemplate redisTemplate) {
        return new RedisCacheHealthService(redisTemplate);
    }

}

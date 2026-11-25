package team.carrypigeon.backend.infrastructure.service.cache.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;

/** 配置 Redis 连接与字符串模板，供 prod 和 dev 的缓存及验证码存储使用。 */
@Configuration(proxyBeanMethods = false)
@EnvironmentComponent(profiles = {PROD, DEV})
public class CacheServiceConfiguration {

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
}

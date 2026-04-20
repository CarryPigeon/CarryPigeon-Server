package team.carrypigeon.backend.infrastructure.service.cache.impl.health;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealth;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;

/**
 * Redis 缓存健康检查实现。
 * 职责：通过 ping 验证 Redis 服务是否可用。
 * 边界：Redis 连接细节停留在 impl 内部。
 */
public class RedisCacheHealthService implements CacheHealthService {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheHealthService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CacheHealth check() {
        try {
            String pong = redisTemplate.execute((RedisConnection connection) -> connection.ping());
            return new CacheHealth("PONG".equalsIgnoreCase(pong), "redis ping completed");
        } catch (RuntimeException ex) {
            return new CacheHealth(false, "redis ping failed: " + ex.getClass().getSimpleName());
        }
    }
}

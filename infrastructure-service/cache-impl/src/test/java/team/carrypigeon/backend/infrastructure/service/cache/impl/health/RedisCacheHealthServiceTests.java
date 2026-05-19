package team.carrypigeon.backend.infrastructure.service.cache.impl.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RedisCacheHealthService 契约测试。
 * 职责：验证 Redis 健康检查对 ping 结果与异常场景的稳定映射。
 * 边界：不连接真实 Redis，只验证健康检查语义。
 */
@Tag("contract")
class RedisCacheHealthServiceTests {

    /**
     * 验证 PONG 响应会映射为 available=true。
     */
    @Test
    @DisplayName("check pong response returns available health")
    void check_pongResponse_returnsAvailableHealth() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");
        RedisCacheHealthService service = new RedisCacheHealthService(redisTemplate);

        CacheHealth result = service.check();

        assertTrue(result.available());
        assertEquals("redis ping completed", result.message());
    }

    /**
     * 验证非 PONG 响应会映射为 available=false 但保持稳定说明。
     */
    @Test
    @DisplayName("check non pong response returns unavailable health")
    void check_nonPongResponse_returnsUnavailableHealth() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("NOPE");
        RedisCacheHealthService service = new RedisCacheHealthService(redisTemplate);

        CacheHealth result = service.check();

        assertFalse(result.available());
        assertEquals("redis ping completed", result.message());
    }

    /**
     * 验证 Redis 客户端异常时会返回 unavailable 健康结果而不是抛出异常。
     */
    @Test
    @DisplayName("check client failure returns unavailable health")
    void check_clientFailure_returnsUnavailableHealth() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisCallback.class))).thenThrow(new IllegalStateException("ping failed"));
        RedisCacheHealthService service = new RedisCacheHealthService(redisTemplate);

        CacheHealth result = service.check();

        assertFalse(result.available());
        assertEquals("redis ping failed: IllegalStateException", result.message());
    }
}

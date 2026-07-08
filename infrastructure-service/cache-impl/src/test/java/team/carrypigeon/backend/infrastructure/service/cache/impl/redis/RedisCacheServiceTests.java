package team.carrypigeon.backend.infrastructure.service.cache.impl.redis;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.impl.config.CacheServiceProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisCacheService 契约测试。
 * 职责：验证 Redis 字符串缓存实现对 cache-api 稳定契约的映射与异常语义。
 * 边界：不连接真实 Redis，只验证模板调用参数和返回/失败行为。
 */
@Tag("contract")
class RedisCacheServiceTests {

    private static final CacheServiceProperties PROPERTIES = new CacheServiceProperties(true, Duration.ofMinutes(5));

    /**
     * 验证读取存在 key 时会返回 Optional.of(value)。
     */
    @Test
    @DisplayName("get existing key returns cached value")
    void get_existingKey_returnsCachedValue() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:room:1")).thenReturn("hello");
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        Optional<String> result = service.get("chat:room:1");

        assertTrue(result.isPresent());
        assertEquals("hello", result.orElseThrow());
    }

    /**
     * 验证读取未命中 key 时会返回 Optional.empty()。
     */
    @Test
    @DisplayName("get missing key returns empty optional")
    void get_missingKey_returnsEmptyOptional() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:room:1")).thenReturn(null);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        Optional<String> result = service.get("chat:room:1");

        assertTrue(result.isEmpty());
    }

    /**
     * 验证读取失败时会包装成稳定缓存异常。
     */
    @Test
    @DisplayName("get client failure wraps cache service exception")
    void get_clientFailure_wrapsCacheServiceException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        RuntimeException cause = new RuntimeException("redis read failed");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:room:1")).thenThrow(cause);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        CacheServiceException exception = assertThrows(CacheServiceException.class, () -> service.get("chat:room:1"));

        assertEquals("failed to read cache entry", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证写入时若调用方未传 TTL，会回退到默认 TTL。
     */
    @Test
    @DisplayName("set null ttl uses configured default ttl")
    void set_nullTtl_usesConfiguredDefaultTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        service.set("chat:room:1", "hello", null);

        verify(valueOperations).set("chat:room:1", "hello", Duration.ofMinutes(5));
    }

    /**
     * 验证写入时若调用方传入 TTL，会使用调用方给定值。
     */
    @Test
    @DisplayName("set explicit ttl uses provided ttl")
    void set_explicitTtl_usesProvidedTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        service.set("chat:room:1", "hello", Duration.ofSeconds(30));

        verify(valueOperations).set("chat:room:1", "hello", Duration.ofSeconds(30));
    }

    /**
     * 验证写入失败时会包装成稳定缓存异常。
     */
    @Test
    @DisplayName("set client failure wraps cache service exception")
    void set_clientFailure_wrapsCacheServiceException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        RuntimeException cause = new RuntimeException("redis write failed");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(cause).when(valueOperations).set("chat:room:1", "hello", Duration.ofMinutes(5));
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        CacheServiceException exception = assertThrows(
                CacheServiceException.class,
                () -> service.set("chat:room:1", "hello", null)
        );

        assertEquals("failed to write cache entry", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证删除失败时会包装成稳定缓存异常。
     */
    @Test
    @DisplayName("delete client failure wraps cache service exception")
    void delete_clientFailure_wrapsCacheServiceException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RuntimeException cause = new RuntimeException("redis down");
        doThrow(cause).when(redisTemplate).delete("chat:room:1");
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        CacheServiceException exception = assertThrows(
                CacheServiceException.class,
                () -> service.delete("chat:room:1")
        );

        assertEquals("failed to delete cache entry", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证原子消费命中时会通过 Lua 脚本完成比较删除，并返回成功。
     */
    @Test
    @DisplayName("consume if equals matching value executes script and returns true")
    void consumeIfEquals_matchingValue_executesScriptAndReturnsTrue() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Boolean>>any(),
                eq(List.of("chat:code:1")),
                eq("123456")
        )).thenReturn(Boolean.TRUE);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        boolean result = service.consumeIfEquals("chat:code:1", "123456");

        assertTrue(result);
    }

    /**
     * 验证原子消费未命中或值不匹配时会返回 false。
     */
    @Test
    @DisplayName("consume if equals mismatch returns false")
    void consumeIfEquals_mismatch_returnsFalse() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Boolean>>any(),
                eq(List.of("chat:code:1")),
                eq("654321")
        )).thenReturn(Boolean.FALSE);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        boolean result = service.consumeIfEquals("chat:code:1", "654321");

        assertFalse(result);
    }

    /**
     * 验证原子消费失败时会包装成稳定缓存异常。
     */
    @Test
    @DisplayName("consume if equals client failure wraps cache service exception")
    void consumeIfEquals_clientFailure_wrapsCacheServiceException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RuntimeException cause = new RuntimeException("redis script failed");
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Boolean>>any(),
                eq(List.of("chat:code:1")),
                eq("123456")
        )).thenThrow(cause);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        CacheServiceException exception = assertThrows(
                CacheServiceException.class,
                () -> service.consumeIfEquals("chat:code:1", "123456")
        );

        assertEquals("failed to consume cache entry", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证 exists 在 Redis 返回 null 时会稳定映射为 false。
     */
    @Test
    @DisplayName("exists null response returns false")
    void exists_nullResponse_returnsFalse() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey("chat:room:1")).thenReturn(null);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        boolean result = service.exists("chat:room:1");

        assertFalse(result);
    }

    /**
     * 验证 exists 失败时会包装成稳定缓存异常。
     */
    @Test
    @DisplayName("exists client failure wraps cache service exception")
    void exists_clientFailure_wrapsCacheServiceException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RuntimeException cause = new RuntimeException("redis exists failed");
        when(redisTemplate.hasKey("chat:room:1")).thenThrow(cause);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        CacheServiceException exception = assertThrows(CacheServiceException.class, () -> service.exists("chat:room:1"));

        assertEquals("failed to check cache entry", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证缓存服务会在访问 Redis 前拒绝非法输入。
     */
    @Test
    @DisplayName("invalid input is rejected before redis access")
    void invalidInput_rejectedBeforeRedisAccess() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisCacheService service = new RedisCacheService(redisTemplate, PROPERTIES);

        IllegalArgumentException blankKeyGetException = assertThrows(IllegalArgumentException.class, () -> service.get(" "));
        IllegalArgumentException nullKeyDeleteException = assertThrows(
                IllegalArgumentException.class,
                () -> service.delete((String) null)
        );
        IllegalArgumentException nullValueSetException = assertThrows(
                IllegalArgumentException.class,
                () -> service.set("chat:room:1", null, Duration.ofMinutes(1))
        );
        IllegalArgumentException invalidTtlSetException = assertThrows(
                IllegalArgumentException.class,
                () -> service.set("chat:room:1", "hello", Duration.ZERO)
        );

        assertEquals("cache key must not be blank", blankKeyGetException.getMessage());
        assertEquals("cache key must not be blank", nullKeyDeleteException.getMessage());
        assertEquals("cache value must not be null", nullValueSetException.getMessage());
        assertEquals("cache ttl must be positive", invalidTtlSetException.getMessage());
        verifyNoInteractions(redisTemplate);
    }
}

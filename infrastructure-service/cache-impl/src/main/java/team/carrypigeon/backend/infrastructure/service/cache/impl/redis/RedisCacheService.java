package team.carrypigeon.backend.infrastructure.service.cache.impl.redis;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.config.CacheServiceProperties;

/**
 * Redis 字符串缓存实现。
 * 职责：基于 StringRedisTemplate 实现 cache-api 定义的字符串缓存契约。
 * 边界：Redis 访问细节封装在 impl 内，不向上层泄露。
 */
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;
    private final CacheServiceProperties properties;

    public RedisCacheService(StringRedisTemplate redisTemplate, CacheServiceProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<String> get(String key) {
        requireKey(key);
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to read cache entry", ex);
        }
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        requireKey(key);
        requireValue(value);
        Duration effectiveTtl = ttl == null ? properties.defaultTtl() : ttl;
        requireTtl(effectiveTtl);
        try {
            redisTemplate.opsForValue().set(key, value, effectiveTtl);
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to write cache entry", ex);
        }
    }

    @Override
    public void delete(String key) {
        requireKey(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to delete cache entry", ex);
        }
    }

    @Override
    public boolean exists(String key) {
        requireKey(key);
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to check cache entry", ex);
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("cache key must not be blank");
        }
    }

    private static void requireValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("cache value must not be null");
        }
    }

    private static void requireTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("cache ttl must be positive");
        }
    }
}

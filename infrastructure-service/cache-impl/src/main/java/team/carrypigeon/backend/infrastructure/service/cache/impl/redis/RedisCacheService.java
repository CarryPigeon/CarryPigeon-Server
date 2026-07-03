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

    /**
     * 读取指定缓存键的字符串值。
     * 输入：业务层生成的完整缓存键。
     * 输出：命中时返回缓存值，未命中时返回空。
     *
     * @param key 缓存键
     * @return 缓存字符串值
     * @throws IllegalArgumentException 缓存键为空白时抛出
     * @throws CacheServiceException Redis 访问失败时抛出
     */
    @Override
    public Optional<String> get(String key) {
        requireKey(key);
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to read cache entry", ex);
        }
    }

    /**
     * 写入带过期时间的字符串缓存。
     * 输入：缓存键、缓存值以及期望 TTL；当 TTL 为空时退回默认配置。
     * 副作用：覆盖同键已有值并重置过期时间。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间；为空时使用默认 TTL
     * @throws IllegalArgumentException 缓存键为空白、值为空或 TTL 非正时抛出
     * @throws CacheServiceException Redis 写入失败时抛出
     */
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

    /**
     * 删除指定缓存键。
     * 输入：业务层生成的完整缓存键。
     * 副作用：删除 Redis 中对应条目；键不存在时保持幂等。
     *
     * @param key 缓存键
     * @throws IllegalArgumentException 缓存键为空白时抛出
     * @throws CacheServiceException Redis 删除失败时抛出
     */
    @Override
    public void delete(String key) {
        requireKey(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to delete cache entry", ex);
        }
    }

    /**
     * 检查指定缓存键是否存在。
     * 输入：业务层生成的完整缓存键。
     * 输出：仅当 Redis 明确返回存在时返回 true。
     *
     * @param key 缓存键
     * @return 缓存键是否存在
     * @throws IllegalArgumentException 缓存键为空白时抛出
     * @throws CacheServiceException Redis 查询失败时抛出
     */
    @Override
    public boolean exists(String key) {
        requireKey(key);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
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

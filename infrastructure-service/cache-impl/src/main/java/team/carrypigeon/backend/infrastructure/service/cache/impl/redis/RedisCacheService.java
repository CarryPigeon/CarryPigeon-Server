package team.carrypigeon.backend.infrastructure.service.cache.impl.redis;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.config.RedisCacheProperties;

/**
 * Redis 字符串缓存实现。
 * 职责：基于 StringRedisTemplate 实现 cache-api 定义的字符串缓存契约。
 * 边界：Redis 访问细节封装在 impl 内，不向上层泄露。
 */
@EnvironmentComponent(profiles = {PROD, DEV})
public class RedisCacheService implements CacheService {

    private static final DefaultRedisScript<Boolean> CONSUME_IF_EQUALS_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """,
            Boolean.class
    );

    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL_SCRIPT = new DefaultRedisScript<>(
        """
        local current = redis.call('INCR', KEYS[1])
        -- PTTL < 0 表示键没有 TTL（-1）或不存在（-2）：两种情况都应该补上过期时间
        if redis.call('PTTL', KEYS[1]) < 0 then
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
        end
        return current
        """,
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RedisCacheProperties properties;

    public RedisCacheService(StringRedisTemplate redisTemplate, RedisCacheProperties properties) {
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
     * 批量读取缓存键对应的字符串值。
     * 输入：一组业务层生成的完整缓存键。
     * 输出：命中键到缓存值的映射，未命中键不进入结果；保持入参顺序。
     * 实现：使用 Redis MGET 在一次往返中完成批量读取。
     *
     * @param keys 缓存键集合
     * @return 命中键到缓存值的映射
     * @throws IllegalArgumentException 任意缓存键为空白时抛出
     * @throws CacheServiceException Redis 访问失败时抛出
     */
    @Override
    public Map<String, String> getAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        for (String key : keys) {
            requireKey(key);
        }
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            Map<String, String> result = new LinkedHashMap<>();
            int index = 0;
            for (String key : keys) {
                if (index < values.size()) {
                    String value = values.get(index);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
                index++;
            }
            return result;
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to read cache entries", ex);
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

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        requireKey(key);
        requireValue(value);
        Duration effectiveTtl = ttl == null ? properties.defaultTtl() : ttl;
        requireTtl(effectiveTtl);
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, effectiveTtl));
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to write cache entry", ex);
        }
    }

    @Override
    public long increment(String key, Duration ttl) {
        requireKey(key);
        requireTtl(ttl);
        try {
            return redisTemplate.execute(
                INCREMENT_WITH_TTL_SCRIPT,
                List.of(key),
                String.valueOf(ttl.toMillis())
            );
        } catch (CacheServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to increment cache counter", ex);
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
     * 原子比较并删除指定缓存键。
     * 原因：邮箱验证码等一次性凭证需要避免 get 后 delete 的并发重复消费窗口。
     */
    @Override
    public boolean consumeIfEquals(String key, String expectedValue) {
        requireKey(key);
        requireValue(expectedValue);
        try {
            return redisTemplate.execute(
                CONSUME_IF_EQUALS_SCRIPT,
                List.of(key),
                expectedValue
            );
        } catch (RuntimeException ex) {
            throw new CacheServiceException("failed to consume cache entry", ex);
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
            return redisTemplate.hasKey(key);
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

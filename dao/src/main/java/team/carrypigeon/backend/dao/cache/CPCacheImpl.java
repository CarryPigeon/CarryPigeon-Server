package team.carrypigeon.backend.dao.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.cache.CPCache;

import java.util.concurrent.TimeUnit;

/**
 * {@link CPCache} 的 Redis 实现（String → String）。
 * <p>
 * 基于 {@link StringRedisTemplate}，所有 TTL 单位均为秒。
 */
@Slf4j
@Service
public class CPCacheImpl implements CPCache {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建缓存实现（由 Spring 注入 Redis 客户端）。
     */
    public CPCacheImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void set(String key, String value, int expireTime) {
        log.debug("CPCacheImpl#set - key={}, expireTime={}", key, expireTime);
        stringRedisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        log.debug("CPCacheImpl#get - key={}", key);
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public String getAndDelete(String key) {
        log.debug("CPCacheImpl#getAndDelete - key={}", key);
        return stringRedisTemplate.opsForValue().getAndDelete(key);
    }

    @Override
    public String getAndSet(String key, String value, int expireTime) {
        log.debug("CPCacheImpl#getAndSet - key={}, expireTime={}", key, expireTime);
        String result = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.opsForValue().set(key, value, expireTime,TimeUnit.SECONDS);
        return result;
    }

    @Override
    public boolean setIfAbsent(String key, String value, int expireTime) {
        log.debug("CPCacheImpl#setIfAbsent - key={}, expireTime={}", key, expireTime);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Math.max(1, expireTime), TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public long increment(String key, long delta, int expireTime) {
        log.debug("CPCacheImpl#increment - key={}, delta={}, expireTime={}", key, delta, expireTime);
        Long value = stringRedisTemplate.opsForValue().increment(key, delta);
        if (value == null) {
            return 0L;
        }
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            stringRedisTemplate.expire(key, Math.max(1, expireTime), TimeUnit.SECONDS);
        }
        return value;
    }

    @Override
    public boolean exists(String key) {
        boolean exists = stringRedisTemplate.hasKey(key);
        log.debug("CPCacheImpl#exists - key={}, exists={}", key, exists);
        return exists;
    }

    @Override
    public boolean delete(String key) {
        boolean deleted = stringRedisTemplate.delete(key);
        log.debug("CPCacheImpl#delete - key={}, deleted={}", key, deleted);
        return deleted;
    }
}

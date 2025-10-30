package team.carrypigeon.backend.dao.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.dao.cache.CPCache;

import java.util.concurrent.TimeUnit;

@Service
public class CPCacheImpl implements CPCache {

    private final StringRedisTemplate stringRedisTemplate;

    public CPCacheImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void set(String key, String value, int expireTime) {
        stringRedisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public String getAndDelete(String key) {
        return stringRedisTemplate.opsForValue().getAndDelete(key);
    }

    @Override
    public String getAndSet(String key, String value, int expireTime) {
        String result = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.opsForValue().set(key, value, expireTime,TimeUnit.SECONDS);
        return result;
    }

    @Override
    public boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }
}

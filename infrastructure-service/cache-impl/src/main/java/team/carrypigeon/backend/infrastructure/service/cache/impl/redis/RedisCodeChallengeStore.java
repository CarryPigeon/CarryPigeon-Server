package team.carrypigeon.backend.infrastructure.service.cache.impl.redis;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CodeChallengeStore;

/**
 * Redis 单键挑战状态机；Lua 保证次数限制和消费在多实例之间只有一个判定结果。
 * 消费或耗尽后保留终态至原 TTL 到期，重发整体替换，清理按 challengeId 比较。
 */
public class RedisCodeChallengeStore implements CodeChallengeStore {

    private static final DefaultRedisScript<Long> REPLACE = new DefaultRedisScript<>("""
            redis.call('DEL', KEYS[1])
            redis.call('HSET', KEYS[1], 'id', ARGV[1], 'code', ARGV[2], 'remaining', ARGV[3], 'state', 'active', 'delivery', 'pending')
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'id') ~= ARGV[1] then return 4 end
            local state = redis.call('HGET', KEYS[1], 'state')
            if state == 'consumed' then return 3 end
            if state == 'exhausted' then return 2 end
            if state ~= 'active' then return 4 end
            if redis.call('HGET', KEYS[1], 'code') == ARGV[2] then
                redis.call('HSET', KEYS[1], 'state', 'consumed')
                redis.call('HDEL', KEYS[1], 'code')
                return 0
            end
            local remaining = redis.call('HINCRBY', KEYS[1], 'remaining', -1)
            if remaining <= 0 then
                redis.call('HSET', KEYS[1], 'state', 'exhausted')
                redis.call('HDEL', KEYS[1], 'code')
                return 2
            end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> COMPLETE_DELIVERY = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'id') ~= ARGV[1] then return 0 end
            redis.call('HSET', KEYS[1], 'delivery', ARGV[2])
            if ARGV[2] == 'failed' and redis.call('HGET', KEYS[1], 'state') == 'active' then
                redis.call('HSET', KEYS[1], 'state', 'delivery_failed')
                redis.call('HDEL', KEYS[1], 'code')
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCodeChallengeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void replace(String key, String challengeId, String code, int attempts, Duration ttl) {
        requireKey(key);
        if (challengeId == null || challengeId.isBlank() || code == null || code.isBlank()
                || attempts <= 0 || ttl == null || ttl.toMillis() <= 0) {
            throw new IllegalArgumentException("challenge ID, code, attempts and TTL must be valid");
        }
        try {
            redisTemplate.execute(REPLACE, List.of(key), challengeId, code,
                    Integer.toString(attempts), Long.toString(ttl.toMillis()));
        } catch (RuntimeException exception) {
            throw new CacheServiceException("failed to replace code challenge", exception);
        }
    }

    @Override
    public Result verify(String key, String code) {
        requireKey(key);
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        try {
            // 先取得本次校验所属的挑战；重发若发生在两次命令之间，Lua 拒绝修改新挑战。
            String challengeId = (String) redisTemplate.opsForHash().get(key, "id");
            if (challengeId == null) {
                return Result.UNAVAILABLE;
            }
            Long result = redisTemplate.execute(VERIFY, List.of(key), challengeId, code);
            if (result == null) {
                throw new CacheServiceException("code challenge verification returned no result");
            }
            return switch (result.intValue()) {
                case 0 -> Result.VERIFIED;
                case 1 -> Result.INVALID;
                case 2 -> Result.EXHAUSTED;
                case 3 -> Result.CONSUMED;
                case 4 -> Result.UNAVAILABLE;
                default -> throw new CacheServiceException("unknown code challenge result");
            };
        } catch (CacheServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new CacheServiceException("failed to verify code challenge", exception);
        }
    }

    @Override
    public void completeDelivery(String key, String challengeId, boolean accepted) {
        requireKey(key);
        if (challengeId == null || challengeId.isBlank()) {
            throw new IllegalArgumentException("challenge ID must not be blank");
        }
        try {
            redisTemplate.execute(COMPLETE_DELIVERY, List.of(key), challengeId, accepted ? "accepted" : "failed");
        } catch (RuntimeException exception) {
            throw new CacheServiceException("failed to record code delivery result", exception);
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("challenge key must not be blank");
        }
    }
}

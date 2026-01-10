package team.carrypigeon.backend.dao.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTests {

    @Test
    void redisTemplate_shouldConfigureSerializersAndFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConfig config = new RedisConfig(new ObjectMapper());

        RedisTemplate<String, Object> redisTemplate = config.redisTemplate(factory);
        assertNotNull(redisTemplate);
        assertSame(factory, redisTemplate.getConnectionFactory());
        assertNotNull(redisTemplate.getKeySerializer());
        assertNotNull(redisTemplate.getValueSerializer());
        assertNotNull(redisTemplate.getHashKeySerializer());
        assertNotNull(redisTemplate.getHashValueSerializer());
    }

    @Test
    void stringRedisTemplate_shouldUseFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConfig config = new RedisConfig(new ObjectMapper());

        StringRedisTemplate template = config.stringRedisTemplate(factory);
        assertNotNull(template);
        assertSame(factory, template.getConnectionFactory());
    }

    @Test
    void operationsBeans_shouldDelegateToTemplates() {
        RedisConfig config = new RedisConfig(new ObjectMapper());

        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, Object> listOps = mock(ListOperations.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, Object> setOps = mock(SetOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        assertSame(hashOps, config.hashOperations(redisTemplate));
        assertSame(listOps, config.listOperations(redisTemplate));
        assertSame(setOps, config.setOperations(redisTemplate));
        assertSame(zSetOps, config.zSetOperations(redisTemplate));

        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        assertSame(valueOps, config.valueOperations(stringRedisTemplate));
    }

    @Test
    void cacheManager_shouldBuild() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConfig config = new RedisConfig(new ObjectMapper());

        RedisCacheManager cacheManager = config.cacheManager(factory);
        assertNotNull(cacheManager);
    }
}

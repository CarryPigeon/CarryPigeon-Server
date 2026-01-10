package team.carrypigeon.backend.dao.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CPCacheImplTests {

    @Test
    void set_shouldDelegateToStringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);

        CPCacheImpl cache = new CPCacheImpl(template);
        cache.set("k", "v", 3);

        verify(ops, times(1)).set("k", "v", 3, TimeUnit.SECONDS);
    }

    @Test
    void get_shouldReturnValueFromTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn("v");

        CPCacheImpl cache = new CPCacheImpl(template);
        assertEquals("v", cache.get("k"));
    }

    @Test
    void getAndDelete_shouldReturnValueFromTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.getAndDelete("k")).thenReturn("v");

        CPCacheImpl cache = new CPCacheImpl(template);
        assertEquals("v", cache.getAndDelete("k"));
    }

    @Test
    void getAndSet_shouldReturnPreviousValueAndSetNewValue() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn("old");

        CPCacheImpl cache = new CPCacheImpl(template);
        assertEquals("old", cache.getAndSet("k", "new", 5));
        verify(ops, times(1)).set("k", "new", 5, TimeUnit.SECONDS);
    }

    @Test
    void exists_shouldDelegateToHasKey() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.hasKey("k")).thenReturn(true);

        CPCacheImpl cache = new CPCacheImpl(template);
        assertTrue(cache.exists("k"));
    }

    @Test
    void delete_shouldDelegateToDelete() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.delete("k")).thenReturn(true);

        CPCacheImpl cache = new CPCacheImpl(template);
        assertTrue(cache.delete("k"));
    }
}


package team.carrypigeon.backend.infrastructure.service.cache.memory;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.TEST;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;
import team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;

/** 测试字符串缓存，以实例锁保证比较删除和计数更新原子完成。 */
@EnvironmentComponent(profiles = TEST)
public class MemoryCache implements CacheService {
    private record Entry(String value, Instant expiresAt) {}
    private final Map<String, Entry> entries = new HashMap<>();
    private final Clock clock;

    public MemoryCache(Clock clock) {
        this.clock = clock;
    }

    private Entry current(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("cache key must not be blank");
        }
        Entry entry = entries.get(key);
        if (entry != null && !clock.instant().isBefore(entry.expiresAt())) {
            entries.remove(key);
            return null;
        }
        return entry;
    }

    private Instant expiresAt(Duration ttl) {
        if (ttl == null || !ttl.isPositive()) {
            throw new IllegalArgumentException("cache TTL must be positive");
        }
        return clock.instant().plus(ttl);
    }

    @Override
    public synchronized Optional<String> get(String key) {
        return Optional.ofNullable(current(key)).map(Entry::value);
    }

    @Override
    public synchronized Map<String, String> getAll(Collection<String> keys) {
        Map<String, String> result = new HashMap<>();
        keys.forEach(key -> get(key).ifPresent(value -> result.put(key, value)));
        return result;
    }

    @Override
    public synchronized void set(String key, String value, Duration ttl) {
        current(key);
        entries.put(key, new Entry(Objects.requireNonNull(value), expiresAt(ttl == null ? Duration.ofMinutes(5) : ttl)));
    }

    @Override
    public synchronized boolean setIfAbsent(String key, String value, Duration ttl) {
        Objects.requireNonNull(value);
        Instant deadline = expiresAt(ttl == null ? Duration.ofMinutes(5) : ttl);
        if (current(key) != null) {
            return false;
        }
        entries.put(key, new Entry(value, deadline));
        return true;
    }

    @Override
    public synchronized long increment(String key, Duration ttl) {
        Instant deadline = expiresAt(ttl);
        Entry previous = current(key);
        try {
            long next = previous == null ? 1 : Math.incrementExact(Long.parseLong(previous.value()));
            entries.put(key, new Entry(Long.toString(next), previous == null ? deadline : previous.expiresAt()));
            return next;
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new CacheServiceException("failed to increment cache counter", failure);
        }
    }

    @Override
    public synchronized void delete(String key) {
        current(key);
        entries.remove(key);
    }

    @Override
    public synchronized boolean consumeIfEquals(String key, String expectedValue) {
        Objects.requireNonNull(expectedValue);
        if (get(key).filter(expectedValue::equals).isEmpty()) {
            return false;
        }
        entries.remove(key);
        return true;
    }

    @Override
    public synchronized boolean exists(String key) {
        return current(key) != null;
    }
}

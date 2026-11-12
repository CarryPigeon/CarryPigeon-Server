package team.carrypigeon.backend.chat.domain.features.verification.domain.service.limit;

import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.limit.RateLimitApi;
import team.carrypigeon.backend.infrastructure.service.cache.api.service.CacheService;

import java.time.Duration;

@Component
public class RateLimitImpl implements RateLimitApi {

    private final CacheService cacheService;


    public RateLimitImpl(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean tryAcquire(String key, long seconds) {
        return cacheService.setIfAbsent(key, "1", Duration.ofSeconds(seconds));
    }

    @Override
    public long count(String key, long seconds) {
        return cacheService.increment(key, Duration.ofSeconds(seconds));
    }
}

package team.carrypigeon.backend.infrastructure.service.cache.api.model;

import java.time.Duration;

/**
 * 缓存存活时间值对象。
 * 职责：统一表达缓存过期时间语义。
 * 约束：TTL 必须大于 0。
 *
 * @param value 缓存存活时间
 */
public record CacheTtl(Duration value) {

    public CacheTtl {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("cache ttl must be positive");
        }
    }
}

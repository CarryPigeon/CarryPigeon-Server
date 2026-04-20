package team.carrypigeon.backend.infrastructure.service.cache.api.model;

/**
 * 缓存键值对象。
 * 职责：表达跨模块缓存键契约，避免上层直接依赖 Redis key 细节。
 * 约束：缓存键不能为空白字符串。
 *
 * @param value 缓存键文本
 */
public record CacheKey(String value) {

    public CacheKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cache key must not be blank");
        }
    }
}

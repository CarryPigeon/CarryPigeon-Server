package team.carrypigeon.backend.infrastructure.service.cache.api.model;

/**
 * 字符串缓存条目。
 * 职责：表达一个待写入或已读取的字符串缓存值。
 * 边界：不承载对象序列化规则，第一阶段只保存字符串值。
 *
 * @param key 缓存键
 * @param value 字符串缓存值
 * @param ttl 缓存存活时间
 */
public record CacheEntry(CacheKey key, String value, CacheTtl ttl) {

    public CacheEntry {
        if (key == null) {
            throw new IllegalArgumentException("cache key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("cache value must not be null");
        }
        if (ttl == null) {
            throw new IllegalArgumentException("cache ttl must not be null");
        }
    }
}

package team.carrypigeon.backend.infrastructure.service.cache.api.service;

import java.time.Duration;
import java.util.Optional;

/**
 * 字符串缓存服务抽象。
 * 职责：为上层模块提供最小缓存读写能力。
 * 边界：第一阶段只支持字符串缓存，不暴露 Redis、Lettuce 或序列化实现。
 * 原因：当前稳定 public contract 仅保留 {@code String} / {@code Duration} 主签名，避免在第一阶段继续扩张或引入新的调用方兼容性风险。
 * 约束：key 为空白字符串时必须拒绝；set 的 value 不能为空；ttl 为空时由实现使用稳定默认值，非正 TTL 必须拒绝。
 */
public interface CacheService {

    /**
     * 根据缓存键读取字符串值。
     *
     * @param key 缓存键
     * @return 命中时返回缓存值，未命中时返回空
     * @throws IllegalArgumentException 当缓存键为空白字符串时抛出
     * @throws team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException 当缓存后端访问失败时抛出
     */
    Optional<String> get(String key);

    /**
     * 写入带过期时间的字符串缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 缓存存活时间
     * @throws IllegalArgumentException 当缓存键为空白、缓存值为 null、或 TTL 不为正数时抛出
     * @throws team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException 当缓存后端访问失败时抛出
     */
    void set(String key, String value, Duration ttl);

    /**
     * 删除指定缓存键。
     *
     * @param key 缓存键
     * @throws IllegalArgumentException 当缓存键为空白字符串时抛出
     * @throws team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException 当缓存后端访问失败时抛出
     */
    void delete(String key);

    /**
     * 当缓存值与期望值一致时原子消费该缓存键。
     * 语义：返回 true 表示值匹配且键已被删除；返回 false 表示未命中或值不匹配。
     * 默认实现只表达兼容语义，强一致一次性消费应由具体实现覆盖。
     *
     * @param key 缓存键
     * @param expectedValue 期望缓存值
     * @return 是否匹配并消费成功
     */
    default boolean consumeIfEquals(String key, String expectedValue) {
        if (expectedValue == null) {
            throw new IllegalArgumentException("expected cache value must not be null");
        }
        Optional<String> value = get(key);
        if (value.isEmpty() || !value.get().equals(expectedValue)) {
            return false;
        }
        delete(key);
        return true;
    }

    /**
     * 判断指定缓存键是否存在。
     *
     * @param key 缓存键
     * @return 存在时返回 true
     * @throws IllegalArgumentException 当缓存键为空白字符串时抛出
     * @throws team.carrypigeon.backend.infrastructure.service.cache.api.exception.CacheServiceException 当缓存后端访问失败时抛出
     */
    boolean exists(String key);
}

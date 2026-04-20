package team.carrypigeon.backend.infrastructure.service.cache.api.service;

import java.time.Duration;
import java.util.Optional;

/**
 * 字符串缓存服务抽象。
 * 职责：为上层模块提供最小缓存读写能力。
 * 边界：第一阶段只支持字符串缓存，不暴露 Redis、Lettuce 或序列化实现。
 */
public interface CacheService {

    /**
     * 根据缓存键读取字符串值。
     *
     * @param key 缓存键
     * @return 命中时返回缓存值，未命中时返回空
     */
    Optional<String> get(String key);

    /**
     * 写入带过期时间的字符串缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 缓存存活时间
     */
    void set(String key, String value, Duration ttl);

    /**
     * 删除指定缓存键。
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断指定缓存键是否存在。
     *
     * @param key 缓存键
     * @return 存在时返回 true
     */
    boolean exists(String key);
}

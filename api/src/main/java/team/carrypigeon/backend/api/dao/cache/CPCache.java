package team.carrypigeon.backend.api.dao.cache;

/**
 * 简单 KV 缓存抽象（String → String）。
 * <p>
 * 用途：
 * <ul>
 *     <li>短期状态（例如验证码、一次性 token、限流窗口等）</li>
 *     <li>轻量级跨节点协作（若底层实现为 Redis）</li>
 * </ul>
 *
 * <p>约束：
 * <ul>
 *     <li>expireTime 单位为秒（int）；</li>
 *     <li>返回值语义以实现为准，但接口应尽量保持“无副作用”的直觉。</li>
 * </ul>
 */
public interface CPCache {
    /**
     * 设置缓存（覆盖写）。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间（秒）
     */
    void set(String key, String value,int expireTime);

    /**
     * 获取缓存。
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    String get(String key);
    /**
     * 获取并删除缓存（一次性语义）。
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    String getAndDelete(String key);
    /**
     * 获取旧值并设置新值（并刷新 TTL）。
     *
     * @param key 缓存键
     * @param value 新缓存值
     * @param expireTime 过期时间（秒）
     * @return 旧缓存值，不存在返回 null
     */
    String getAndSet(String key, String value,int expireTime);

    /**
     * 仅当 key 不存在时写入缓存（带 TTL）。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间（秒）
     * @return true 表示写入成功；false 表示 key 已存在
     */
    default boolean setIfAbsent(String key, String value, int expireTime) {
        if (exists(key)) {
            return false;
        }
        set(key, value, expireTime);
        return true;
    }

    /**
     * 原子自增计数（用于限流窗口）。
     * <p>
     * 当 key 首次创建时实现需保证最终具备 TTL（秒）。
     *
     * @param key 缓存键
     * @param delta 增量（通常为 1）
     * @param expireTime 过期时间（秒）
     * @return 自增后的值；失败时返回 0
     */
    default long increment(String key, long delta, int expireTime) {
        if (delta == 0) {
            String current = get(key);
            if (current == null) {
                return 0;
            }
            try {
                return Long.parseLong(current);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        String current = get(key);
        long next = delta;
        if (current != null && !current.isBlank()) {
            try {
                next = Long.parseLong(current) + delta;
            } catch (NumberFormatException ignored) {
                next = delta;
            }
        }
        set(key, Long.toString(next), expireTime);
        return next;
    }

    /**
     * 判断缓存是否存在。
     *
     * @param key 缓存键
     * @return true 表示存在
     */
    boolean exists(String key);
    /**
     * 删除缓存。
     *
     * @param key 缓存键
     * @return true 表示成功删除（或原本不存在且被视为成功）
     */
    boolean delete(String key);
}

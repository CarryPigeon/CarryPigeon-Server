package team.carrypigeon.backend.infrastructure.service.cache.api.health;

/**
 * 缓存健康检查抽象。
 * 职责：为启动层和上层模块提供缓存可用性判断入口。
 * 边界：具体 Redis 检查逻辑由 cache-impl 承担。
 */
public interface CacheHealthService {

    /**
     * 检查缓存服务当前健康状态。
     *
     * @return 缓存健康状态
     */
    CacheHealth check();
}

package team.carrypigeon.backend.infrastructure.service.storage.api.health;

/**
 * 对象存储健康检查抽象。
 * 职责：为启动层和上层模块提供对象存储可用性判断入口。
 * 边界：具体 MinIO 检查逻辑由 storage-impl 承担。
 */
public interface StorageHealthService {

    /**
     * 检查对象存储服务当前健康状态。
     *
     * @return 对象存储健康状态
     */
    StorageHealth check();
}

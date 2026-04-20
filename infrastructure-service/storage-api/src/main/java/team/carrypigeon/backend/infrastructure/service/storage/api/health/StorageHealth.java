package team.carrypigeon.backend.infrastructure.service.storage.api.health;

/**
 * 对象存储服务健康状态。
 * 职责：表达对象存储外部服务当前是否可用。
 * 边界：不暴露 MinIO 或具体存储实现细节。
 *
 * @param available 对象存储服务是否可用
 * @param message 健康状态说明
 */
public record StorageHealth(boolean available, String message) {
}

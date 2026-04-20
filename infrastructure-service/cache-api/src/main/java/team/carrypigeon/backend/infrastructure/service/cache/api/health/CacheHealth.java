package team.carrypigeon.backend.infrastructure.service.cache.api.health;

/**
 * 缓存服务健康状态。
 * 职责：表达缓存外部服务当前是否可用。
 * 边界：不暴露 Redis 或 Lettuce 连接细节。
 *
 * @param available 缓存服务是否可用
 * @param message 健康状态说明
 */
public record CacheHealth(boolean available, String message) {
}

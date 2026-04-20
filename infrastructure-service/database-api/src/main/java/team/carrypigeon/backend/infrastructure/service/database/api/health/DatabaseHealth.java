package team.carrypigeon.backend.infrastructure.service.database.api.health;

/**
 * 数据库服务健康状态。
 * 职责：表达数据库外部服务当前是否可用，以及不可用时的稳定说明。
 * 边界：不暴露 JDBC、连接池或具体数据库驱动细节。
 *
 * @param available 数据库服务是否可用
 * @param message 健康状态说明，供日志和运维定位使用
 */
public record DatabaseHealth(boolean available, String message) {
}

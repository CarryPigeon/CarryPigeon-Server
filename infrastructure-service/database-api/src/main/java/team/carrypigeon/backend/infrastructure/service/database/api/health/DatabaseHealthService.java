package team.carrypigeon.backend.infrastructure.service.database.api.health;

/**
 * 数据库健康检查抽象。
 * 职责：为启动层和业务侧提供数据库服务可用性判断入口。
 * 边界：这里只定义能力契约，具体检查方式由 database-impl 决定。
 */
public interface DatabaseHealthService {

    /**
     * 检查数据库服务当前健康状态。
     *
     * @return 数据库健康状态
     */
    DatabaseHealth check();
}

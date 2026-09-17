package team.carrypigeon.backend.infrastructure.service.database.impl.health;

import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;

/**
 * JDBC 数据库健康检查实现。
 * 职责：通过轻量 SQL 验证数据库连接与查询能力。
 * 边界：异常细节只用于健康消息，不向 API 泄露 JDBC 类型。
 */
public class JdbcDatabaseHealthService implements DatabaseHealthService {

    private static final String HEALTH_QUERY = "SELECT 1";

    private final JdbcClientSupport jdbcClientSupport;

    public JdbcDatabaseHealthService(JdbcClientSupport jdbcClientSupport) {
        this.jdbcClientSupport = jdbcClientSupport;
    }

    /**
     * 执行数据库健康检查。
     * 输出：返回健康状态以及基于健康查询结果生成的诊断消息。
     *
     * @return 数据库健康检查结果
     */
    @Override
    public DatabaseHealth check() {
        try {
            Integer result = jdbcClientSupport.queryInteger(HEALTH_QUERY);
            return new DatabaseHealth(result != null, "database health query completed");
        } catch (RuntimeException ex) {
            return new DatabaseHealth(false, "database health query failed: " + ex.getClass().getSimpleName());
        }
    }
}

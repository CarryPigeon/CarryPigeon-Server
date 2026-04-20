package team.carrypigeon.backend.infrastructure.service.database.impl.health;

import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceProperties;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;

/**
 * JDBC 数据库健康检查实现。
 * 职责：通过轻量 SQL 验证数据库连接与查询能力。
 * 边界：异常细节只用于健康消息，不向 API 泄露 JDBC 类型。
 */
public class JdbcDatabaseHealthService implements DatabaseHealthService {

    private final JdbcClientSupport jdbcClientSupport;
    private final DatabaseServiceProperties properties;

    public JdbcDatabaseHealthService(JdbcClientSupport jdbcClientSupport, DatabaseServiceProperties properties) {
        this.jdbcClientSupport = jdbcClientSupport;
        this.properties = properties;
    }

    @Override
    public DatabaseHealth check() {
        try {
            Integer result = jdbcClientSupport.queryInteger(properties.healthQuery());
            return new DatabaseHealth(result != null, "database health query completed");
        } catch (RuntimeException ex) {
            return new DatabaseHealth(false, "database health query failed: " + ex.getClass().getSimpleName());
        }
    }
}

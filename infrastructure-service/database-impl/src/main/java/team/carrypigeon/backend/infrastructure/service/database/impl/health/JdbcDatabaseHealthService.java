package team.carrypigeon.backend.infrastructure.service.database.impl.health;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;
import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.DEV;

import team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent;

import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * JDBC 数据库健康检查实现。
 * 职责：通过轻量 SQL 验证数据库连接与查询能力。
 */
@EnvironmentComponent(profiles = {PROD, DEV})
public class JdbcDatabaseHealthService implements DatabaseHealthService {

    private static final String HEALTH_QUERY = "SELECT 1";

    private final JdbcClient jdbcClient;

    public JdbcDatabaseHealthService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
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
            Integer result = jdbcClient.sql(HEALTH_QUERY).query(Integer.class).single();
            return new DatabaseHealth(result != null, "database health query completed");
        } catch (RuntimeException ex) {
            return new DatabaseHealth(false, "database health query failed: " + ex.getClass().getSimpleName());
        }
    }
}

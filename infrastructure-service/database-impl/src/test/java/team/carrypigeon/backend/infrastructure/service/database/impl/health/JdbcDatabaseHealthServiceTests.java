package team.carrypigeon.backend.infrastructure.service.database.impl.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealth;
import team.carrypigeon.backend.infrastructure.service.database.impl.config.DatabaseServiceProperties;
import team.carrypigeon.backend.infrastructure.service.database.impl.jdbc.JdbcClientSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JdbcDatabaseHealthService 契约测试。
 * 职责：验证 JDBC 健康检查对查询结果与异常场景的稳定映射。
 * 边界：不访问真实数据库，只验证健康检查语义。
 */
@Tag("contract")
class JdbcDatabaseHealthServiceTests {

    private static final DatabaseServiceProperties PROPERTIES = new DatabaseServiceProperties(true, "SELECT 1");

    /**
     * 验证健康查询返回整数时会映射为 available=true。
     */
    @Test
    @DisplayName("check non null query result returns available health")
    void check_nonNullQueryResult_returnsAvailableHealth() {
        JdbcClientSupport jdbcClientSupport = mock(JdbcClientSupport.class);
        when(jdbcClientSupport.queryInteger("SELECT 1")).thenReturn(1);
        JdbcDatabaseHealthService service = new JdbcDatabaseHealthService(jdbcClientSupport, PROPERTIES);

        DatabaseHealth result = service.check();

        assertTrue(result.available());
        assertEquals("database health query completed", result.message());
    }

    /**
     * 验证健康查询返回 null 时会映射为 available=false 但保持稳定说明。
     */
    @Test
    @DisplayName("check null query result returns unavailable health")
    void check_nullQueryResult_returnsUnavailableHealth() {
        JdbcClientSupport jdbcClientSupport = mock(JdbcClientSupport.class);
        when(jdbcClientSupport.queryInteger("SELECT 1")).thenReturn(null);
        JdbcDatabaseHealthService service = new JdbcDatabaseHealthService(jdbcClientSupport, PROPERTIES);

        DatabaseHealth result = service.check();

        assertFalse(result.available());
        assertEquals("database health query completed", result.message());
    }

    /**
     * 验证健康查询异常时会返回 unavailable 健康结果而不是抛出异常。
     */
    @Test
    @DisplayName("check query failure returns unavailable health")
    void check_queryFailure_returnsUnavailableHealth() {
        JdbcClientSupport jdbcClientSupport = mock(JdbcClientSupport.class);
        when(jdbcClientSupport.queryInteger("SELECT 1")).thenThrow(new IllegalStateException("db down"));
        JdbcDatabaseHealthService service = new JdbcDatabaseHealthService(jdbcClientSupport, PROPERTIES);

        DatabaseHealth result = service.check();

        assertFalse(result.available());
        assertEquals("database health query failed: IllegalStateException", result.message());
    }
}

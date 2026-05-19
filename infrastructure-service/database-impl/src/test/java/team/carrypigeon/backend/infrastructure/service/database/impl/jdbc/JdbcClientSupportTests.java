package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JdbcClientSupport 契约测试。
 * 职责：验证 database-impl 对 Spring JdbcClient 的轻量查询封装行为。
 * 边界：不访问真实数据库，只验证 SQL 与结果映射调用链。
 */
@Tag("contract")
class JdbcClientSupportTests {

    /**
     * 验证查询整数时会沿用给定 SQL 并返回单个整数结果。
     */
    @Test
    @DisplayName("query integer valid sql returns single integer result")
    void queryInteger_validSql_returnsSingleIntegerResult() {
        JdbcClient jdbcClient = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbcClient.sql("SELECT 1").query(Integer.class).single()).thenReturn(1);
        JdbcClientSupport support = new JdbcClientSupport(jdbcClient);

        Integer result = support.queryInteger("SELECT 1");

        assertEquals(1, result);
    }
}

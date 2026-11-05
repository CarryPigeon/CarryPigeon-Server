package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * JDBC 客户端支持入口。
 * 职责：封装 database-impl 内部对 Spring JdbcClient 的访问。
 * 边界：该类型只允许在 database-impl 内使用，不进入 database-api 或 chat-domain。
 */
public class JdbcClientSupport {

    private final JdbcClient jdbcClient;

    public JdbcClientSupport(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * 执行轻量查询并返回单个整数结果。
     *
     * @param sql 查询 SQL
     * @return 查询结果
     */
    public Integer queryInteger(String sql) {
        return jdbcClient.sql(sql).query(Integer.class).single();
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JdbcChannelDatabaseService 契约测试。
 * 职责：验证频道 JDBC 数据库服务的关键查询契约与失败语义。
 * 边界：不访问真实数据库，只验证 JDBC 客户端交互后的稳定行为。
 */
class JdbcChannelDatabaseServiceTests {

    /**
     * 验证查询默认频道时会稳定映射频道记录。
     */
    @Test
    @DisplayName("find default channel existing row maps record")
    void findDefaultChannel_existingRow_mapsRecord() throws Exception {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<ChannelRecord> mappedQuerySpec = mock();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.query(anyRowMapper())).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.optional()).thenReturn(java.util.Optional.of(new ChannelRecord(
                1L,
                1L,
                "public",
                "public",
                true,
                Instant.parse("2026-04-22T00:00:00Z"),
                Instant.parse("2026-04-22T00:00:00Z")
        )));
        JdbcChannelDatabaseService service = new JdbcChannelDatabaseService(jdbcClient);

        ChannelRecord record = service.findDefaultChannel().orElseThrow();

        assertEquals(1L, record.id());
        assertEquals("public", record.name());
        assertEquals(true, record.defaultChannel());
    }

    /**
     * 验证底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find channel by id data access failure wraps database service exception")
    void findById_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenThrow(cause);
        JdbcChannelDatabaseService service = new JdbcChannelDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findById(1L)
        );

        assertEquals("failed to query channel by id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static RowMapper<ChannelRecord> anyRowMapper() {
        return any();
    }
}

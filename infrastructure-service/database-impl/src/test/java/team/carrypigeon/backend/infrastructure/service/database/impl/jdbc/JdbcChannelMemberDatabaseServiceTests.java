package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcChannelMemberDatabaseService 契约测试。
 * 职责：验证频道成员 JDBC 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 JDBC 客户端交互后的稳定行为。
 */
class JdbcChannelMemberDatabaseServiceTests {

    /**
     * 验证存在成员关系时 exists 会返回 true。
     */
    @Test
    @DisplayName("exists existing membership returns true")
    void exists_existingMembership_returnsTrue() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Long> mappedQuerySpec = mock();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(Long.class)).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.single()).thenReturn(1L);
        JdbcChannelMemberDatabaseService service = new JdbcChannelMemberDatabaseService(jdbcClient);

        assertTrue(service.exists(1L, 1001L));
    }

    /**
     * 验证插入成员关系时会绑定完整持久化字段。
     */
    @Test
    @DisplayName("insert valid record binds all fields")
    void insert_validRecord_bindsAllFields() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);
        JdbcChannelMemberDatabaseService service = new JdbcChannelMemberDatabaseService(jdbcClient);

        service.insert(new ChannelMemberRecord(1L, 1001L, Instant.parse("2026-04-22T00:00:00Z")));

        verify(statementSpec).param("channelId", 1L);
        verify(statementSpec).param("accountId", 1001L);
        verify(statementSpec).param(org.mockito.ArgumentMatchers.eq("joinedAt"), any(Timestamp.class));
    }

    /**
     * 验证查询成员账户失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find account ids data access failure wraps database service exception")
    void findAccountIdsByChannelId_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenThrow(cause);
        JdbcChannelMemberDatabaseService service = new JdbcChannelMemberDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findAccountIdsByChannelId(1L)
        );

        assertEquals("failed to query channel member account ids", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

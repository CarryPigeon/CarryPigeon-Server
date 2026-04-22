package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcMessageDatabaseService 契约测试。
 * 职责：验证消息 JDBC 数据库服务的关键读写契约与失败语义。
 * 边界：不访问真实数据库，只验证 JDBC 客户端交互后的稳定行为。
 */
class JdbcMessageDatabaseServiceTests {

    /**
     * 验证插入消息时会绑定完整通用消息字段。
     */
    @Test
    @DisplayName("insert valid record binds all fields")
    void insert_validRecord_bindsAllFields() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);
        JdbcMessageDatabaseService service = new JdbcMessageDatabaseService(jdbcClient);

        service.insert(record());

        verify(statementSpec).param("messageId", 5001L);
        verify(statementSpec).param("serverId", "carrypigeon-local");
        verify(statementSpec).param(org.mockito.ArgumentMatchers.eq("createdAt"), any(Timestamp.class));
    }

    /**
     * 验证按频道查询历史消息时会稳定映射消息记录。
     */
    @Test
    @DisplayName("find by channel id before existing rows maps records")
    void findByChannelIdBefore_existingRows_mapsRecords() throws Exception {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<MessageRecord> mappedQuerySpec = mock();
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(anyRowMapper())).thenReturn(mappedQuerySpec);
        when(mappedQuerySpec.list()).thenReturn(List.of(record()));
        JdbcMessageDatabaseService service = new JdbcMessageDatabaseService(jdbcClient);

        MessageRecord record = service.findByChannelIdBefore(1L, null, 20).getFirst();

        assertEquals(5001L, record.messageId());
        assertEquals("carrypigeon-local", record.serverId());
    }

    /**
     * 验证查询失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by channel id before data access failure wraps database service exception")
    void findByChannelIdBefore_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenThrow(cause);
        JdbcMessageDatabaseService service = new JdbcMessageDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findByChannelIdBefore(1L, null, 20)
        );

        assertEquals("failed to query channel messages", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static MessageRecord record() {
        return new MessageRecord(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                null,
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z")
        );
    }

    private static RowMapper<MessageRecord> anyRowMapper() {
        return any();
    }
}

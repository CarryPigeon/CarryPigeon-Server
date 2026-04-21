package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcUserProfileDatabaseService 契约测试。
 * 职责：验证用户资料 JDBC 数据库服务的关键失败路径处理。
 * 边界：不访问真实数据库，只验证 JDBC 客户端交互后的稳定异常语义。
 */
class JdbcUserProfileDatabaseServiceTests {

    private static final Instant CREATED_AT = Instant.parse("2026-04-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证按账户 ID 查询时会将数据库行稳定映射为契约记录。
     */
    @Test
    @DisplayName("find by account id existing row maps record")
    void findByAccountId_existingRow_mapsRecord() throws Exception {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<UserProfileRecord> mappedQuerySpec = mock();
        ResultSet resultSet = mock(ResultSet.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param("accountId", 1001L)).thenReturn(statementSpec);
        when(resultSet.getLong("account_id")).thenReturn(1001L);
        when(resultSet.getString("nickname")).thenReturn("carry-user");
        when(resultSet.getString("avatar_url")).thenReturn("https://img.example/avatar.png");
        when(resultSet.getString("bio")).thenReturn("hello world");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(CREATED_AT));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(UPDATED_AT));
        when(statementSpec.query(anyRowMapper())).thenAnswer(invocation -> {
            RowMapper<UserProfileRecord> rowMapper = invocation.getArgument(0);
            UserProfileRecord mappedRecord = rowMapper.mapRow(resultSet, 0);
            when(mappedQuerySpec.optional()).thenReturn(Optional.of(mappedRecord));
            return mappedQuerySpec;
        });
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        UserProfileRecord record = service.findByAccountId(1001L).orElseThrow();

        assertEquals(1001L, record.accountId());
        assertEquals("carry-user", record.nickname());
        assertEquals("https://img.example/avatar.png", record.avatarUrl());
        assertEquals("hello world", record.bio());
        assertEquals(CREATED_AT, record.createdAt());
        assertEquals(UPDATED_AT, record.updatedAt());
    }

    /**
     * 验证查询底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by account id data access failure wraps database service exception")
    void findByAccountId_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenThrow(cause);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findByAccountId(1001L)
        );

        assertEquals("failed to query user profile by account id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证插入资料时会绑定完整持久化字段。
     */
    @Test
    @DisplayName("insert valid record binds all fields")
    void insert_validRecord_bindsAllFields() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        service.insert(record());

        verify(statementSpec).param("accountId", 1001L);
        verify(statementSpec).param("nickname", "carry-user");
        verify(statementSpec).param("avatarUrl", "https://img.example/avatar.png");
        verify(statementSpec).param("bio", "hello world");
        verify(statementSpec).param(eq("createdAt"), any(Timestamp.class));
        verify(statementSpec).param(eq("updatedAt"), any(Timestamp.class));
        verify(statementSpec).update();
    }

    /**
     * 验证插入底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("insert data access failure wraps database service exception")
    void insert_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenThrow(cause);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.insert(record())
        );

        assertEquals("failed to insert user profile", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证更新影响一行时会正常完成。
     */
    @Test
    @DisplayName("update one affected row succeeds")
    void update_oneAffectedRow_succeeds() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        service.update(record());

        verify(statementSpec).param("accountId", 1001L);
        verify(statementSpec).param("nickname", "carry-user");
        verify(statementSpec).param("avatarUrl", "https://img.example/avatar.png");
        verify(statementSpec).param("bio", "hello world");
        verify(statementSpec).param(eq("updatedAt"), any(Timestamp.class));
        verify(statementSpec).update();
    }

    /**
     * 验证更新影响行数为零时会抛出稳定数据库服务异常。
     */
    @Test
    @DisplayName("update zero affected rows throws database service exception")
    void update_zeroAffectedRows_throwsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(0);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(record())
        );

        assertEquals("user profile update affected no rows", exception.getMessage());
    }

    /**
     * 验证更新底层数据库失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("update data access failure wraps database service exception")
    void update_dataAccessFailure_wrapsDatabaseServiceException() {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.update()).thenThrow(cause);
        JdbcUserProfileDatabaseService service = new JdbcUserProfileDatabaseService(jdbcClient);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.update(record())
        );

        assertEquals("failed to update user profile", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static UserProfileRecord record() {
        return new UserProfileRecord(
                1001L,
                "carry-user",
                "https://img.example/avatar.png",
                "hello world",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static RowMapper<UserProfileRecord> anyRowMapper() {
        return any();
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.AuthRefreshSessionMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusAuthRefreshSessionDatabaseService 契约测试。
 * 职责：验证刷新会话 MyBatis-Plus 数据库服务的关键查询、写入、撤销与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
class MybatisPlusAuthRefreshSessionDatabaseServiceTests {

    private static final Instant EXPIRES_AT = Instant.parse("2026-05-01T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-04-20T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-21T12:00:00Z");

    /**
     * 验证按会话 ID 查询时会稳定映射刷新会话记录。
     */
    @Test
    @DisplayName("find by id existing row maps record")
    void findById_existingRow_mapsRecord() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        when(mapper.selectById(9001L)).thenReturn(entity());
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        AuthRefreshSessionRecord record = service.findById(9001L).orElseThrow();

        assertEquals(9001L, record.id());
        assertEquals(1001L, record.accountId());
        assertEquals("refresh-hash", record.refreshTokenHash());
        assertEquals(EXPIRES_AT, record.expiresAt());
        assertEquals(false, record.revoked());
        assertEquals(CREATED_AT, record.createdAt());
        assertEquals(UPDATED_AT, record.updatedAt());
    }

    /**
     * 验证查询失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("find by id data access failure wraps database service exception")
    void findById_dataAccessFailure_wrapsDatabaseServiceException() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.selectById(9001L)).thenThrow(cause);
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.findById(9001L)
        );

        assertEquals("failed to query auth refresh session by id", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证写入刷新会话时会委托 mapper 持久化完整记录。
     */
    @Test
    @DisplayName("insert valid record maps all fields")
    void insert_validRecord_mapsAllFields() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        when(mapper.insert(any(AuthRefreshSessionEntity.class))).thenReturn(1);
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        service.insert(record());

        verify(mapper).insert(any(AuthRefreshSessionEntity.class));
    }

    /**
     * 验证写入失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("insert data access failure wraps database service exception")
    void insert_dataAccessFailure_wrapsDatabaseServiceException() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.insert(any(AuthRefreshSessionEntity.class))).thenThrow(cause);
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.insert(record())
        );

        assertEquals("failed to insert auth refresh session", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证撤销刷新会话时会委托 mapper 使用既有 SQL 语义更新记录。
     */
    @Test
    @DisplayName("revoke valid id delegates to mapper")
    void revoke_validId_delegatesToMapper() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        when(mapper.revokeById(9001L)).thenReturn(1);
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        service.revoke(9001L);

        verify(mapper).revokeById(9001L);
    }

    /**
     * 验证撤销失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("revoke data access failure wraps database service exception")
    void revoke_dataAccessFailure_wrapsDatabaseServiceException() {
        AuthRefreshSessionMapper mapper = mock(AuthRefreshSessionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.revokeById(9001L)).thenThrow(cause);
        MybatisPlusAuthRefreshSessionDatabaseService service = new MybatisPlusAuthRefreshSessionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.revoke(9001L)
        );

        assertEquals("failed to revoke auth refresh session", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static AuthRefreshSessionRecord record() {
        return new AuthRefreshSessionRecord(9001L, 1001L, "refresh-hash", EXPIRES_AT, false, CREATED_AT, UPDATED_AT);
    }

    private static AuthRefreshSessionEntity entity() {
        AuthRefreshSessionEntity entity = new AuthRefreshSessionEntity();
        entity.setId(9001L);
        entity.setAccountId(1001L);
        entity.setRefreshTokenHash("refresh-hash");
        entity.setExpiresAt(EXPIRES_AT);
        entity.setRevoked(false);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}

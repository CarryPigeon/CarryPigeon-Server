package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelAuditLogEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelAuditLogMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusChannelAuditLogDatabaseService 契约测试。
 * 职责：验证频道审计日志 MyBatis-Plus 数据库服务的关键写入契约与失败语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusChannelAuditLogDatabaseServiceTests {

    /**
     * 验证追加审计日志时会下发给 mapper。
     */
    @Test
    @DisplayName("insert valid record delegates to mapper")
    void insert_validRecord_delegatesToMapper() {
        ChannelAuditLogMapper channelAuditLogMapper = mock(ChannelAuditLogMapper.class);
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);

        service.insert(new ChannelAuditLogWriteRecord(
                7001L,
                1L,
                1001L,
                "MEMBER_BANNED",
                1002L,
                "{}",
                Instant.parse("2026-04-24T12:30:00Z")
        ));

        verify(channelAuditLogMapper).insert(any(ChannelAuditLogEntity.class));
    }

    /**
     * 验证追加审计日志失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("insert data access failure wraps database service exception")
    void insert_dataAccessFailure_wrapsDatabaseServiceException() {
        ChannelAuditLogMapper channelAuditLogMapper = mock(ChannelAuditLogMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        doThrow(cause).when(channelAuditLogMapper).insert(any(ChannelAuditLogEntity.class));
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.insert(new ChannelAuditLogWriteRecord(7001L, 1L, 1001L, "MEMBER_BANNED", 1002L, "{}", Instant.parse("2026-04-24T12:30:00Z")))
        );

        assertEquals("failed to insert channel audit log", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("list query maps entities to records")
    void listQuery_mapsEntitiesToRecords() {
        ChannelAuditLogMapper channelAuditLogMapper = mock(ChannelAuditLogMapper.class);
        ChannelAuditLogEntity entity = new ChannelAuditLogEntity();
        entity.setAuditId(7001L);
        entity.setChannelId(9L);
        entity.setActorAccountId(1001L);
        entity.setActionType("MEMBER_BANNED");
        entity.setMetadata("{}");
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        when(channelAuditLogMapper.list(eq(null), eq(50), eq(null), eq(null), eq(null), eq(null), eq(null))).thenReturn(List.of(entity));
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);

        ChannelAuditLogReadRecord record = service.list(null, 50, null, null, null, null, null).getFirst();

        assertEquals(7001L, record.auditId());
        assertEquals("MEMBER_BANNED", record.actionType());
    }
}

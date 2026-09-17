package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelAuditLogEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelAuditLogMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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
        ChannelAuditLogWriteRecord record = new ChannelAuditLogWriteRecord(
                7001L,
                1L,
                1001L,
                "MEMBER_BANNED",
                1002L,
                "{}",
                Instant.parse("2026-04-24T12:30:00Z")
        );

        service.insert(record);

        ArgumentCaptor<ChannelAuditLogEntity> captor = ArgumentCaptor.forClass(ChannelAuditLogEntity.class);
        verify(channelAuditLogMapper).insert(captor.capture());
        ChannelAuditLogEntity entity = captor.getValue();
        assertEquals(record.auditId(), entity.getAuditId());
        assertEquals(record.channelId(), entity.getChannelId());
        assertEquals(record.actorAccountId(), entity.getActorAccountId());
        assertEquals(record.actionType(), entity.getActionType());
        assertEquals(record.targetAccountId(), entity.getTargetAccountId());
        assertEquals(record.metadata(), entity.getMetadata());
        assertEquals(record.createdAt(), entity.getCreatedAt());
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

    /**
     * 验证存在审计日志时轻量存在性查询返回 true。
     */
    @Test
    @DisplayName("exists by channel id existing row returns true")
    void existsByChannelId_existingRow_returnsTrue() {
        ChannelAuditLogMapper mapper = mock(ChannelAuditLogMapper.class);
        when(mapper.existsByChannelId(1L)).thenReturn(true);
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(mapper);

        assertTrue(service.existsByChannelId(1L));
        verify(mapper).existsByChannelId(1L);
    }

    /**
     * 验证 `listQuery` 在 `mapsEntitiesToRecords` 场景下的测试契约。
     */
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
        when(channelAuditLogMapper.listByChannelIds(eq(null), eq(50), eq(null), eq(null), eq(null), eq(null), eq(null))).thenReturn(List.of(entity));
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(channelAuditLogMapper);

        ChannelAuditLogReadRecord record = service.list(null, 50, null, null, null, null, null).getFirst();

        assertEquals(7001L, record.auditId());
        assertEquals("MEMBER_BANNED", record.actionType());
    }

    /** 验证超大频道集合分片查询后仍按审计 ID 全局排序并截断。 */
    @Test
    @DisplayName("list by channel ids oversized collection merges batches")
    void listByChannelIds_oversizedCollection_mergesBatches() {
        ChannelAuditLogMapper mapper = mock(ChannelAuditLogMapper.class);
        when(mapper.listByChannelIds(eq(null), eq(2), any(Collection.class), eq(null), eq(null), eq(null), eq(null)))
                .thenAnswer(invocation -> {
                    Collection<Long> channelIds = invocation.getArgument(2);
                    ChannelAuditLogEntity entity = new ChannelAuditLogEntity();
                    entity.setAuditId(channelIds.contains(1L) ? 7001L : 7003L);
                    entity.setChannelId(channelIds.iterator().next());
                    entity.setActorAccountId(1001L);
                    entity.setActionType("MEMBER_MUTED");
                    entity.setMetadata("{}");
                    entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
                    return List.of(entity);
                });
        MybatisPlusChannelAuditLogDatabaseService service = new MybatisPlusChannelAuditLogDatabaseService(mapper);
        List<Long> channelIds = LongStream.rangeClosed(1L, 501L).boxed().toList();

        List<ChannelAuditLogReadRecord> result = service.listByChannelIds(
                null, 2, channelIds, null, null, null, null
        );

        assertEquals(List.of(7003L, 7001L), result.stream().map(ChannelAuditLogReadRecord::auditId).toList());
        verify(mapper, times(2)).listByChannelIds(eq(null), eq(2), any(Collection.class), eq(null), eq(null), eq(null), eq(null));
    }
}

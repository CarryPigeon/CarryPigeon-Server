package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MentionMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MybatisPlusMentionDatabaseService 契约测试。
 * 职责：验证提及数据库服务的字段映射、读取结果与失败包装语义。
 * 边界：不访问真实数据库，只验证 mapper 交互后的稳定行为。
 */
@Tag("contract")
class MybatisPlusMentionDatabaseServiceTests {

    /**
     * 验证插入提及时会完整映射 record 字段到实体。
     */
    @Test
    @DisplayName("insert valid record maps all fields")
    void insert_validRecord_mapsAllFields() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.insert(any(MentionEntity.class))).thenReturn(1);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        service.insert(record());

        ArgumentCaptor<MentionEntity> captor = ArgumentCaptor.forClass(MentionEntity.class);
        verify(mapper).insert(captor.capture());
        MentionEntity entity = captor.getValue();
        assertEquals(11L, entity.getMentionId());
        assertEquals(9L, entity.getChannelId());
        assertEquals(5001L, entity.getMessageId());
        assertEquals(1002L, entity.getFromAccountId());
        assertEquals("user", entity.getTargetType());
        assertEquals(1001L, entity.getTargetAccountId());
        assertEquals(Instant.parse("2026-04-24T12:00:00Z"), entity.getCreatedAt());
        assertEquals(false, entity.getRead());
    }

    /**
     * 验证 `listByAccountId` 在 `mapsEntitiesToRecords` 场景下的测试契约。
     */
    @Test
    @DisplayName("list by account id maps entities to records")
    void listByAccountId_mapsEntitiesToRecords() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.listByAccountId(1001L, 88L, 20, true, 9L)).thenReturn(List.of(entity()));
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        MentionRecord record = service.listByAccountId(1001L, 88L, 20, true, 9L).getFirst();

        assertEquals(11L, record.mentionId());
        assertEquals(9L, record.channelId());
        assertEquals(5001L, record.messageId());
        assertEquals(1002L, record.fromAccountId());
        assertEquals("user", record.targetType());
        assertEquals(1001L, record.targetAccountId());
        assertEquals(Instant.parse("2026-04-24T12:00:00Z"), record.createdAt());
        assertEquals(false, record.read());
    }

    /**
     * 验证查询失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("list by account id data access failure wraps database service exception")
    void listByAccountId_dataAccessFailure_wrapsDatabaseServiceException() {
        MentionMapper mapper = mock(MentionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.listByAccountId(1001L, 88L, 20, true, 9L)).thenThrow(cause);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.listByAccountId(1001L, 88L, 20, true, 9L)
        );

        assertEquals("failed to query mentions", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证按消息删除提及时会委托 mapper 执行依赖清理。
     */
    @Test
    @DisplayName("delete by message id delegates to mapper")
    void deleteByMessageId_delegatesToMapper() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.deleteByMessageId(5001L)).thenReturn(2);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        service.deleteByMessageId(5001L);

        verify(mapper).deleteByMessageId(5001L);
    }

    /**
     * 验证单条已读更新时会返回 mapper 的更新结果。
     */
    @Test
    @DisplayName("mark as read delegates to mapper")
    void markAsRead_delegatesToMapper() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.markAsRead(1001L, 11L)).thenReturn(1);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        boolean result = service.markAsRead(1001L, 11L);

        assertEquals(true, result);
        verify(mapper).markAsRead(1001L, 11L);
    }

    /**
     * 验证单条已读更新失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("mark as read data access failure wraps database service exception")
    void markAsRead_dataAccessFailure_wrapsDatabaseServiceException() {
        MentionMapper mapper = mock(MentionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.markAsRead(1001L, 11L)).thenThrow(cause);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.markAsRead(1001L, 11L)
        );

        assertEquals("failed to mark mention as read", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证批量已读更新时会返回 mapper 更新数量。
     */
    @Test
    @DisplayName("mark all as read delegates to mapper")
    void markAllAsRead_delegatesToMapper() {
        MentionMapper mapper = mock(MentionMapper.class);
        when(mapper.markAllAsRead(1001L, 88L, 9L)).thenReturn(3);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        int result = service.markAllAsRead(1001L, 88L, 9L);

        assertEquals(3, result);
        verify(mapper).markAllAsRead(1001L, 88L, 9L);
    }

    /**
     * 验证批量已读更新失败时会包装成稳定数据库服务异常。
     */
    @Test
    @DisplayName("mark all as read data access failure wraps database service exception")
    void markAllAsRead_dataAccessFailure_wrapsDatabaseServiceException() {
        MentionMapper mapper = mock(MentionMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("database down");
        when(mapper.markAllAsRead(1001L, 88L, 9L)).thenThrow(cause);
        MybatisPlusMentionDatabaseService service = new MybatisPlusMentionDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(
                DatabaseServiceException.class,
                () -> service.markAllAsRead(1001L, 88L, 9L)
        );

        assertEquals("failed to batch mark mentions as read", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static MentionRecord record() {
        return new MentionRecord(11L, 9L, 5001L, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:00:00Z"), false);
    }

    private static MentionEntity entity() {
        MentionEntity entity = new MentionEntity();
        entity.setMentionId(11L);
        entity.setChannelId(9L);
        entity.setMessageId(5001L);
        entity.setFromAccountId(1002L);
        entity.setTargetType("user");
        entity.setTargetAccountId(1001L);
        entity.setCreatedAt(Instant.parse("2026-04-24T12:00:00Z"));
        entity.setRead(Boolean.FALSE);
        return entity;
    }
}

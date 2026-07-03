package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataRetrievalFailureException;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelReadStateMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * `MybatisPlusChannelReadStateDatabaseService` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class MybatisPlusChannelReadStateDatabaseServiceTests {

    /**
     * 验证 `findByChannelAndAccount` 在 `mapsRecord` 场景下的测试契约。
     */
    @Test
    @DisplayName("find by channel and account maps record")
    void findByChannelAndAccount_mapsRecord() {
        ChannelReadStateMapper mapper = mock(ChannelReadStateMapper.class);
        when(mapper.findByChannelIdAndAccountId(1L, 1001L)).thenReturn(entity());
        MybatisPlusChannelReadStateDatabaseService service = new MybatisPlusChannelReadStateDatabaseService(mapper);

        ChannelReadStateRecord record = service.findByChannelIdAndAccountId(1L, 1001L).orElseThrow();

        assertEquals(5001L, record.lastReadMessageId());
    }

    /**
     * 验证 `upsert` 在 `delegatesToMapper` 场景下的测试契约。
     */
    @Test
    @DisplayName("upsert delegates to mapper")
    void upsert_delegatesToMapper() {
        ChannelReadStateMapper mapper = mock(ChannelReadStateMapper.class);
        when(mapper.upsertState(any(ChannelReadStateEntity.class))).thenReturn(1);
        MybatisPlusChannelReadStateDatabaseService service = new MybatisPlusChannelReadStateDatabaseService(mapper);

        service.upsert(record());

        ArgumentCaptor<ChannelReadStateEntity> captor = ArgumentCaptor.forClass(ChannelReadStateEntity.class);
        verify(mapper).upsertState(captor.capture());
        ChannelReadStateEntity entity = captor.getValue();
        assertEquals(1L, entity.getChannelId());
        assertEquals(1001L, entity.getAccountId());
        assertEquals(5001L, entity.getLastReadMessageId());
        assertEquals(Instant.parse("2026-04-22T00:00:00Z"), entity.getLastReadTime());
        assertEquals(Instant.parse("2026-04-22T00:00:00Z"), entity.getCreatedAt());
        assertEquals(Instant.parse("2026-04-22T00:00:00Z"), entity.getUpdatedAt());
    }

    /**
     * 验证 `listUnreads` 在 `mapsProjections` 场景下的测试契约。
     */
    @Test
    @DisplayName("list unreads maps projections")
    void listUnreads_mapsProjections() {
        ChannelReadStateMapper mapper = mock(ChannelReadStateMapper.class);
        ChannelReadStateMapper.UnreadProjection projection = mock(ChannelReadStateMapper.UnreadProjection.class);
        when(projection.getChannelId()).thenReturn(9L);
        when(projection.getUnreadCount()).thenReturn(3L);
        when(projection.getLastReadTime()).thenReturn(Instant.parse("2026-04-22T00:00:00Z"));
        when(mapper.listUnreadsByAccountId(1001L)).thenReturn(List.of(projection));
        MybatisPlusChannelReadStateDatabaseService service = new MybatisPlusChannelReadStateDatabaseService(mapper);

        ChannelUnreadRecord record = service.listUnreadsByAccountId(1001L).getFirst();

        assertEquals(9L, record.channelId());
        assertEquals(3L, record.unreadCount());
    }

    /**
     * 验证 `findByChannelAndAccount` 在 `wrapsDataAccessFailure` 场景下的测试契约。
     */
    @Test
    @DisplayName("find by channel and account wraps data access failure")
    void findByChannelAndAccount_wrapsDataAccessFailure() {
        ChannelReadStateMapper mapper = mock(ChannelReadStateMapper.class);
        DataRetrievalFailureException cause = new DataRetrievalFailureException("boom");
        when(mapper.findByChannelIdAndAccountId(1L, 1001L)).thenThrow(cause);
        MybatisPlusChannelReadStateDatabaseService service = new MybatisPlusChannelReadStateDatabaseService(mapper);

        DatabaseServiceException exception = assertThrows(DatabaseServiceException.class, () -> service.findByChannelIdAndAccountId(1L, 1001L));

        assertEquals("failed to query channel read state", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static ChannelReadStateRecord record() {
        return new ChannelReadStateRecord(1L, 1001L, 5001L, Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"));
    }

    private static ChannelReadStateEntity entity() {
        ChannelReadStateEntity entity = new ChannelReadStateEntity();
        entity.setChannelId(1L);
        entity.setAccountId(1001L);
        entity.setLastReadMessageId(5001L);
        entity.setLastReadTime(Instant.parse("2026-04-22T00:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-04-22T00:00:00Z"));
        return entity;
    }
}

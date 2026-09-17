package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelPinEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.ChannelPinMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * `MybatisPlusChannelPinDatabaseService` 契约测试。
 * 职责：验证当前测试类覆盖对象的关键成功路径、失败路径或边界行为。
 */

@Tag("contract")
class MybatisPlusChannelPinDatabaseServiceTests {

    /**
     * 验证 `insert` 在 `delegatesToMapper` 场景下的测试契约。
     */
    @Test
    @DisplayName("insert delegates to mapper")
    void insert_delegatesToMapper() {
        ChannelPinMapper mapper = mock(ChannelPinMapper.class);
        when(mapper.insert(any(ChannelPinEntity.class))).thenReturn(1);
        MybatisPlusChannelPinDatabaseService service = new MybatisPlusChannelPinDatabaseService(mapper);
        ChannelPinRecord record = new ChannelPinRecord(7001L, 1L, 5001L, 1001L, "note", Instant.parse("2026-04-22T00:00:00Z"));

        service.insert(record);

        ArgumentCaptor<ChannelPinEntity> captor = ArgumentCaptor.forClass(ChannelPinEntity.class);
        verify(mapper).insert(captor.capture());
        ChannelPinEntity entity = captor.getValue();
        assertEquals(record.pinId(), entity.getPinId());
        assertEquals(record.channelId(), entity.getChannelId());
        assertEquals(record.messageId(), entity.getMessageId());
        assertEquals(record.pinnedByAccountId(), entity.getPinnedByAccountId());
        assertEquals(record.note(), entity.getNote());
        assertEquals(record.pinnedAt(), entity.getPinnedAt());
    }

    /**
     * 验证 `findByChannel` 在 `returnsPinRecords` 场景下的测试契约。
     */
    @Test
    @DisplayName("find by channel returns pin records")
    void findByChannel_returnsPinRecords() {
        ChannelPinMapper mapper = mock(ChannelPinMapper.class);
        ChannelPinEntity entity = new ChannelPinEntity();
        entity.setPinId(7001L);
        entity.setChannelId(1L);
        entity.setMessageId(5001L);
        entity.setPinnedByAccountId(1001L);
        entity.setNote("note");
        entity.setPinnedAt(Instant.parse("2026-04-22T00:00:00Z"));
        when(mapper.findByChannelIdBefore(1L, null, 20)).thenReturn(List.of(entity));
        MybatisPlusChannelPinDatabaseService service = new MybatisPlusChannelPinDatabaseService(mapper);

        ChannelPinRecord record = service.findByChannelIdBefore(1L, null, 20).getFirst();

        assertEquals(5001L, record.messageId());
        assertEquals(7001L, record.pinId());
    }

    /** 验证未达上限时会在锁定频道记录后插入新置顶。 */
    @Test
    @DisplayName("replace within limit available slot inserts pin")
    void replaceWithinLimit_availableSlot_insertsPin() {
        ChannelPinMapper mapper = mock(ChannelPinMapper.class);
        when(mapper.lockMessageIdsByChannelId(1L)).thenReturn(List.of(4001L));
        MybatisPlusChannelPinDatabaseService service = new MybatisPlusChannelPinDatabaseService(mapper);
        ChannelPinRecord record = new ChannelPinRecord(7001L, 1L, 5001L, 1001L, "note", Instant.parse("2026-04-22T00:00:00Z"));

        assertTrue(service.replaceWithinLimit(record, 2L));

        verify(mapper).insert(any(ChannelPinEntity.class));
    }

    /** 验证达到上限且目标消息未置顶时不会写入。 */
    @Test
    @DisplayName("replace within limit full channel rejects insert")
    void replaceWithinLimit_fullChannel_rejectsInsert() {
        ChannelPinMapper mapper = mock(ChannelPinMapper.class);
        when(mapper.lockMessageIdsByChannelId(1L)).thenReturn(List.of(4001L, 4002L));
        MybatisPlusChannelPinDatabaseService service = new MybatisPlusChannelPinDatabaseService(mapper);
        ChannelPinRecord record = new ChannelPinRecord(7001L, 1L, 5001L, 1001L, "note", Instant.parse("2026-04-22T00:00:00Z"));

        assertFalse(service.replaceWithinLimit(record, 2L));
    }

}

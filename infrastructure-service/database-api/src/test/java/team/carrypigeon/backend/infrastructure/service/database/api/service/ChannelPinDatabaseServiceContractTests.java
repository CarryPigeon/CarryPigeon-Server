package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
/**
 * ChannelPinDatabaseService 契约测试。
 * 职责：锁定频道置顶记录查询、写入、删除、列表和计数的数据库抽象语义。
 * 边界：不验证具体数据库实现，只验证 database-api 对调用方暴露的最小契约。
 */
@Tag("contract")
class ChannelPinDatabaseServiceContractTests {

    private static final ChannelPinRecord RECORD = new ChannelPinRecord(7001L, 1L, 5001L, 1001L, "note", Instant.parse("2026-04-22T00:00:00Z"));

    /**
     * 验证查询指定频道消息的置顶记录会传递频道 ID 和消息 ID。
     */
    @Test
    @DisplayName("find by channel and message existing pin returns record")
    void findByChannelIdAndMessageId_existingPin_returnsRecord() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();

        ChannelPinRecord result = service.findByChannelIdAndMessageId(1L, 5001L).orElseThrow();

        assertSame(RECORD, result);
        assertEquals(1L, service.lastFindChannelId);
        assertEquals(5001L, service.lastFindMessageId);
    }

    /**
     * 验证写入置顶记录时会把完整记录传递给实现方。
     */
    @Test
    @DisplayName("insert record passes complete pin record")
    void insert_record_passesCompletePinRecord() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    /**
     * 验证删除置顶记录时会传递频道 ID 和消息 ID。
     */
    @Test
    @DisplayName("delete pin passes channel and message ids")
    void delete_pin_passesChannelAndMessageIds() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();

        service.delete(1L, 5001L);

        assertEquals(1L, service.lastDeleteChannelId);
        assertEquals(5001L, service.lastDeleteMessageId);
    }

    /**
     * 验证频道置顶列表查询会传递游标和数量参数。
     */
    @Test
    @DisplayName("find by channel before cursor returns pin records")
    void findByChannelIdBefore_cursor_returnsPinRecords() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();

        ChannelPinRecord result = service.findByChannelIdBefore(1L, 5001L, 20).getFirst();

        assertSame(RECORD, result);
        assertEquals(1L, service.lastListChannelId);
        assertEquals(5001L, service.lastCursorMessageId);
        assertEquals(20, service.lastLimit);
    }

    /**
     * 验证频道置顶计数会按频道 ID 返回数量。
     */
    @Test
    @DisplayName("count by channel returns pin count")
    void countByChannelId_channel_returnsPinCount() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();

        long result = service.countByChannelId(1L);

        assertEquals(2L, result);
        assertEquals(1L, service.lastCountChannelId);
    }

    /**
     * RecordingChannelPinDatabaseService 测试替身。
     * 职责：记录接口入参，使测试只验证 database-api 抽象契约。
     */
    private static class RecordingChannelPinDatabaseService implements ChannelPinDatabaseService {
        private ChannelPinRecord insertedRecord;
        private long lastFindChannelId;
        private long lastFindMessageId;
        private long lastDeleteChannelId;
        private long lastDeleteMessageId;
        private long lastListChannelId;
        private Long lastCursorMessageId;
        private int lastLimit;
        private long lastCountChannelId;

        @Override
        public Optional<ChannelPinRecord> findByChannelIdAndMessageId(long channelId, long messageId) {
            this.lastFindChannelId = channelId;
            this.lastFindMessageId = messageId;
            return Optional.of(RECORD);
        }

        @Override
        public void insert(ChannelPinRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void delete(long channelId, long messageId) {
            this.lastDeleteChannelId = channelId;
            this.lastDeleteMessageId = messageId;
        }

        @Override
        public List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            this.lastListChannelId = channelId;
            this.lastCursorMessageId = cursorMessageId;
            this.lastLimit = limit;
            return List.of(RECORD);
        }

        @Override
        public long countByChannelId(long channelId) {
            this.lastCountChannelId = channelId;
            return 2L;
        }
    }
}

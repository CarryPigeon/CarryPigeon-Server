package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
/**
 * MentionDatabaseService 契约测试。
 * 职责：锁定提及记录写入、列表查询与已读标记的数据库抽象语义。
 * 边界：不验证具体数据库实现，只验证调用方和实现方之间的参数与记录契约。
 */
@Tag("contract")
class MentionDatabaseServiceContractTests {

    private static final MentionRecord RECORD = new MentionRecord(1L, 9L, 5001L, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:00:00Z"), false);

    /**
     * 验证写入提及记录时会把完整记录传递给实现方。
     */
    @Test
    @DisplayName("insert record passes complete mention record")
    void insert_record_passesCompleteMentionRecord() {
        RecordingMentionDatabaseService service = new RecordingMentionDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    /**
     * 验证按账户查询提及时会传递游标、数量、未读过滤和频道过滤条件。
     */
    @Test
    @DisplayName("list by account filters returns mention records")
    void listByAccountId_filters_returnsMentionRecords() {
        RecordingMentionDatabaseService service = new RecordingMentionDatabaseService();

        MentionRecord result = service.listByAccountId(1001L, 11L, 20, true, 9L).getFirst();

        assertSame(RECORD, result);
        assertEquals(1001L, service.lastListAccountId);
        assertEquals(11L, service.lastCursorMentionId);
        assertEquals(20, service.lastLimit);
        assertEquals(true, service.lastUnreadOnly);
        assertEquals(9L, service.lastChannelId);
    }

    /**
     * 验证单条已读标记会返回实现方的更新结果。
     */
    @Test
    @DisplayName("mark as read existing mention returns true")
    void markAsRead_existingMention_returnsTrue() {
        RecordingMentionDatabaseService service = new RecordingMentionDatabaseService();

        boolean result = service.markAsRead(1001L, 11L);

        assertEquals(true, result);
        assertEquals(1001L, service.lastReadAccountId);
        assertEquals(11L, service.lastReadMentionId);
    }

    /**
     * 验证批量已读标记会传递账户、游标和频道过滤条件。
     */
    @Test
    @DisplayName("mark all as read filters returns updated count")
    void markAllAsRead_filters_returnsUpdatedCount() {
        RecordingMentionDatabaseService service = new RecordingMentionDatabaseService();

        int result = service.markAllAsRead(1001L, 88L, 9L);

        assertEquals(3, result);
        assertEquals(1001L, service.lastMarkAllAccountId);
        assertEquals(88L, service.lastBeforeMentionId);
        assertEquals(9L, service.lastMarkAllChannelId);
    }

    /**
     * RecordingMentionDatabaseService 测试替身。
     * 职责：记录接口入参，使测试只验证 database-api 抽象契约。
     */
    private static class RecordingMentionDatabaseService implements MentionDatabaseService {
        private MentionRecord insertedRecord;
        private long lastListAccountId;
        private Long lastCursorMentionId;
        private int lastLimit;
        private boolean lastUnreadOnly;
        private Long lastChannelId;
        private long lastReadAccountId;
        private long lastReadMentionId;
        private long lastMarkAllAccountId;
        private Long lastBeforeMentionId;
        private Long lastMarkAllChannelId;

        @Override
        public void insert(MentionRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void deleteByMessageId(long messageId) {
        }

        @Override
        public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
            this.lastListAccountId = accountId;
            this.lastCursorMentionId = cursorMentionId;
            this.lastLimit = limit;
            this.lastUnreadOnly = unreadOnly;
            this.lastChannelId = channelId;
            return List.of(RECORD);
        }

        @Override
        public boolean markAsRead(long accountId, long mentionId) {
            this.lastReadAccountId = accountId;
            this.lastReadMentionId = mentionId;
            return true;
        }

        @Override
        public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
            this.lastMarkAllAccountId = accountId;
            this.lastBeforeMentionId = beforeMentionId;
            this.lastMarkAllChannelId = channelId;
            return 3;
        }
    }
}

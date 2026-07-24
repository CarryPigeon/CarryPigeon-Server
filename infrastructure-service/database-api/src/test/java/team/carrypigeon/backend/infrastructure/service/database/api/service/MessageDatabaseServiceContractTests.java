package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MessageDatabaseService 行为契约测试。
 * 职责：锁定 canonical 消息数据库抽象的 after 查询、高级搜索 fallback 与读写语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class MessageDatabaseServiceContractTests {

    private static final MessageRecord RECORD = new MessageRecord(
            5001L,
            1001L,
            9L,
            "Core:Text",
            "1.0.0",
            "{\"text\":\"hello\"}",
            Instant.parse("2026-04-22T00:00:00Z"),
            "[]",
            "hello",
            "sent"
    );

    /**
     * 验证未覆盖默认 after 查询能力时会立即 fail-fast。
     */
    @Test
    @DisplayName("find by channel id after default implementation throws unsupported operation")
    void findByChannelIdAfter_defaultImplementation_throwsUnsupportedOperation() {
        MessageDatabaseService service = new MinimalMessageDatabaseService();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> service.findByChannelIdAfter(9L, 5001L, 20)
        );

        assertEquals("message after query is not supported", exception.getMessage());
    }

    /**
     * 验证默认高级搜索会回退到基础关键字搜索实现。
     */
    @Test
    @DisplayName("search by channel id advanced default implementation delegates to basic search")
    void searchByChannelId_advancedDefaultImplementation_delegatesToBasicSearch() {
        RecordingMessageDatabaseService service = new RecordingMessageDatabaseService();

        List<MessageRecord> records = service.searchByChannelId(9L, "hello", 4000L, 1001L, "text", 4500L, 3000L, 20);

        assertEquals(List.of(RECORD), records);
        assertEquals(9L, service.searchedChannelId);
        assertEquals("hello", service.searchedKeyword);
        assertEquals(20, service.searchedLimit);
    }

    /**
     * 验证实现方可接收插入记录。
     */
    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingMessageDatabaseService service = new RecordingMessageDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    /**
     * 验证实现方可接收更新记录。
     */
    @Test
    @DisplayName("update overriding implementation receives record")
    void update_overridingImplementation_receivesRecord() {
        RecordingMessageDatabaseService service = new RecordingMessageDatabaseService();

        service.update(RECORD);

        assertSame(RECORD, service.updatedRecord);
    }

    /**
     * `MinimalMessageDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class MinimalMessageDatabaseService implements MessageDatabaseService {

        @Override
        public void insert(MessageRecord record) {
        }

        @Override
        public Optional<MessageRecord> findById(long messageId) {
            return Optional.empty();
        }

        @Override
        public void update(MessageRecord record) {
        }

        @Override
        public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return List.of();
        }

        @Override
        public List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit) {
            return List.of();
        }
    }

    /**
     * `RecordingMessageDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingMessageDatabaseService extends MinimalMessageDatabaseService {

        private MessageRecord insertedRecord;
        private MessageRecord updatedRecord;
        private long searchedChannelId;
        private String searchedKeyword;
        private int searchedLimit;

        @Override
        public void insert(MessageRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void update(MessageRecord record) {
            this.updatedRecord = record;
        }

        @Override
        public List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit) {
            this.searchedChannelId = channelId;
            this.searchedKeyword = keyword;
            this.searchedLimit = limit;
            return List.of(RECORD);
        }
    }
}

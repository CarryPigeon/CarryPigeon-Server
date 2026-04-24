package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * DatabaseBackedMessageRepository 契约测试。
 * 职责：验证消息仓储适配器与 database-api 契约之间的字段映射稳定性。
 * 边界：不访问真实数据库，只验证仓储层转换语义。
 */
@Tag("contract")
class DatabaseBackedMessageRepositoryTests {

    /**
     * 验证按消息 ID 查询时会把数据库记录稳定映射为领域消息。
     */
    @Test
    @DisplayName("find by id existing record maps to domain model")
    void findById_existingRecord_mapsToDomainModel() {
        RecordingMessageDatabaseService databaseService = new RecordingMessageDatabaseService();
        databaseService.findByIdResult = Optional.of(record());
        DatabaseBackedMessageRepository repository = new DatabaseBackedMessageRepository(databaseService);

        ChannelMessage message = repository.findById(5001L).orElseThrow();

        assertEquals(5001L, message.messageId());
        assertEquals("hello world", message.body());
        assertEquals("sent", message.status());
    }

    /**
     * 验证更新消息时会把领域消息完整转换为数据库契约。
     */
    @Test
    @DisplayName("update recalled message maps to database record")
    void update_recalledMessage_mapsToDatabaseRecord() {
        RecordingMessageDatabaseService databaseService = new RecordingMessageDatabaseService();
        DatabaseBackedMessageRepository repository = new DatabaseBackedMessageRepository(databaseService);
        ChannelMessage recalledMessage = new ChannelMessage(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "[消息已撤回]",
                "[消息已撤回]",
                "",
                null,
                null,
                "recalled",
                Instant.parse("2026-04-22T00:00:00Z")
        );

        ChannelMessage result = repository.update(recalledMessage);

        assertSame(recalledMessage, result);
        assertEquals(5001L, databaseService.updatedRecord.messageId());
        assertEquals("recalled", databaseService.updatedRecord.status());
        assertEquals("", databaseService.updatedRecord.searchableText());
        assertEquals(null, databaseService.updatedRecord.payload());
    }

    /**
     * 验证搜索结果仍通过既有通道内搜索契约映射。
     */
    @Test
    @DisplayName("search by channel id maps records to domain models")
    void searchByChannelId_mapsRecordsToDomainModels() {
        RecordingMessageDatabaseService databaseService = new RecordingMessageDatabaseService();
        databaseService.searchResults = List.of(record());
        DatabaseBackedMessageRepository repository = new DatabaseBackedMessageRepository(databaseService);

        ChannelMessage message = repository.searchByChannelId(1L, "hello", 20).getFirst();

        assertEquals("hello world", message.searchableText());
    }

    private static MessageRecord record() {
        return new MessageRecord(
                5001L,
                "carrypigeon-local",
                1L,
                1L,
                1001L,
                "text",
                "hello world",
                "hello world",
                "hello world",
                null,
                null,
                "sent",
                Instant.parse("2026-04-22T00:00:00Z")
        );
    }

    private static final class RecordingMessageDatabaseService implements MessageDatabaseService {

        private Optional<MessageRecord> findByIdResult = Optional.empty();
        private List<MessageRecord> searchResults = List.of();
        private MessageRecord updatedRecord;

        @Override
        public void insert(MessageRecord record) {
        }

        @Override
        public Optional<MessageRecord> findById(long messageId) {
            return findByIdResult;
        }

        @Override
        public void update(MessageRecord record) {
            this.updatedRecord = record;
        }

        @Override
        public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return List.of();
        }

        @Override
        public List<MessageRecord> searchByChannelId(long channelId, String keyword, int limit) {
            return searchResults;
        }
    }
}

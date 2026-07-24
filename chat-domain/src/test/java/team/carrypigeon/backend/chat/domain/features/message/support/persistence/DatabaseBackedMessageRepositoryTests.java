package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * DatabaseBackedMessageRepository 契约测试。
 * 职责：验证 canonical 领域消息与 database-api JSON 投影的无损映射。
 */
@Tag("contract")
class DatabaseBackedMessageRepositoryTests {

    /**
     * 验证保存消息时 data 和 mentions 以 canonical JSON 写入。
     */
    @Test
    @DisplayName("save maps canonical fields")
    void save_canonicalMessage_mapsCanonicalFields() {
        RecordingService service = new RecordingService();
        DatabaseBackedMessageRepository repository = repository(service);
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper());
        ChannelMessage message = message(MessageStatus.SENT);

        ChannelMessage result = repository.save(message);

        assertSame(message, result);
        assertEquals("Core:ReplyText", service.inserted.domain());
        assertEquals(
                jsonProvider.readTree("{\"content\":{\"text\":\"reply\"},\"reply_to_mid\":\"4999\"}"),
                jsonProvider.readTree(service.inserted.data())
        );
        assertEquals("[\"1002\"]", service.inserted.mentions());
        assertEquals("sent", service.inserted.status());
    }

    /**
     * 验证数据库 JSON 记录可还原为同一 canonical 领域消息。
     */
    @Test
    @DisplayName("find by id maps canonical record")
    void findById_existingRecord_mapsCanonicalRecord() {
        RecordingService service = new RecordingService();
        service.findResult = Optional.of(record());
        DatabaseBackedMessageRepository repository = repository(service);

        ChannelMessage message = repository.findById(5001L).orElseThrow();

        assertEquals("Core:ReplyText", message.domain());
        assertEquals("4999", message.data().get("reply_to_mid"));
        assertEquals(List.of(1002L), message.mentions());
        assertEquals(MessageStatus.SENT, message.status());
    }

    /**
     * 验证撤回更新只写回脱敏后的 canonical 内容。
     */
    @Test
    @DisplayName("update recalled message writes redacted fields")
    void update_recalledMessage_writesRedactedFields() {
        RecordingService service = new RecordingService();
        DatabaseBackedMessageRepository repository = repository(service);

        repository.update(new ChannelMessage(
                5001L, 1001L, 1L, "Core:Text", "1.0.0", Map.of(), BASE_TIME,
                List.of(), "消息已撤回", MessageStatus.RECALLED
        ));

        assertEquals("{}", service.updated.data());
        assertEquals("[]", service.updated.mentions());
        assertEquals("消息已撤回", service.updated.preview());
        assertEquals("recalled", service.updated.status());
    }

    private static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");

    private DatabaseBackedMessageRepository repository(RecordingService service) {
        return new DatabaseBackedMessageRepository(service, new JsonProvider(new ObjectMapper()));
    }

    private ChannelMessage message(MessageStatus status) {
        return new ChannelMessage(
                5001L, 1001L, 1L, "Core:ReplyText", "1.0.0",
                Map.of("content", Map.of("text", "reply"), "reply_to_mid", "4999"),
                BASE_TIME, List.of(1002L), "reply", status
        );
    }

    private MessageRecord record() {
        return new MessageRecord(
                5001L, 1001L, 1L, "Core:ReplyText", "1.0.0",
                "{\"content\":{\"text\":\"reply\"},\"reply_to_mid\":\"4999\"}",
                BASE_TIME, "[\"1002\"]", "reply", "sent"
        );
    }

    private static final class RecordingService implements MessageDatabaseService {

        private Optional<MessageRecord> findResult = Optional.empty();
        private MessageRecord inserted;
        private MessageRecord updated;

        @Override
        public void insert(MessageRecord record) {
            inserted = record;
        }

        @Override
        public Optional<MessageRecord> findById(long messageId) {
            return findResult;
        }

        @Override
        public void update(MessageRecord record) {
            updated = record;
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
}

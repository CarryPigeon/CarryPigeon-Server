package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageIdempotencyDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 数据库消息幂等仓储适配器测试。
 * 职责：验证领域预留与 database-api 最小投影之间无损转换。
 */
@Tag("contract")
class DatabaseBackedMessageIdempotencyRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-07-21T00:00:00Z");

    /**
     * 验证预留读取会保留既有请求指纹和结果消息 ID。
     */
    @Test
    @DisplayName("reserve existing record maps persisted result")
    void reserve_existingRecord_mapsPersistedResult() {
        RecordingService service = new RecordingService();
        service.reserveResult = new MessageIdempotencyRecord(
                1001L, "message.forward.v1", "forward-1", "first", 5001L, BASE_TIME, BASE_TIME.plusSeconds(1)
        );
        DatabaseBackedMessageIdempotencyRepository repository = new DatabaseBackedMessageIdempotencyRepository(service);

        MessageIdempotency result = repository.reserve(new MessageIdempotency(
                1001L, "message.forward.v1", "forward-1", "second", null, BASE_TIME, null
        ));

        assertEquals("first", result.requestFingerprint());
        assertEquals(5001L, result.messageId());
        assertEquals("second", service.reserved.requestFingerprint());
    }

    /**
     * 验证完成操作把领域身份、指纹和消息结果完整委托给 database-api。
     */
    @Test
    @DisplayName("complete reservation delegates all fields")
    void complete_reservation_delegatesAllFields() {
        RecordingService service = new RecordingService();
        DatabaseBackedMessageIdempotencyRepository repository = new DatabaseBackedMessageIdempotencyRepository(service);

        repository.complete(1001L, "message.forward.v1", "forward-1", "fingerprint", 5001L, BASE_TIME);

        assertEquals(1001L, service.completedAccountId);
        assertEquals("message.forward.v1", service.completedOperation);
        assertEquals("forward-1", service.completedKey);
        assertEquals("fingerprint", service.completedFingerprint);
        assertEquals(5001L, service.completedMessageId);
    }

    private static final class RecordingService implements MessageIdempotencyDatabaseService {
        private MessageIdempotencyRecord reserveResult;
        private MessageIdempotencyRecord reserved;
        private long completedAccountId;
        private String completedOperation;
        private String completedKey;
        private String completedFingerprint;
        private long completedMessageId;

        @Override
        public MessageIdempotencyRecord reserve(MessageIdempotencyRecord reservation) {
            this.reserved = reservation;
            return reserveResult == null ? reservation : reserveResult;
        }

        @Override
        public void complete(
                long accountId,
                String operation,
                String idempotencyKey,
                String requestFingerprint,
                long messageId,
                Instant completedAt
        ) {
            this.completedAccountId = accountId;
            this.completedOperation = operation;
            this.completedKey = idempotencyKey;
            this.completedFingerprint = requestFingerprint;
            this.completedMessageId = messageId;
        }
    }
}

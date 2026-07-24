package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 消息幂等数据库服务契约测试。
 * 职责：验证最小持久化契约能够表达预留与结果绑定。
 */
@Tag("contract")
class MessageIdempotencyDatabaseServiceContractTests {

    /**
     * 验证预留与完成方法保留完整的幂等身份和结果字段。
     */
    @Test
    @DisplayName("service reserve and complete preserve contract fields")
    void service_reserveAndComplete_preserveContractFields() {
        RecordingService service = new RecordingService();
        MessageIdempotencyRecord reservation = reservation();

        MessageIdempotencyRecord locked = service.reserve(reservation);
        service.complete(1001L, "message.forward.v1", "forward-1", "abc", 5001L, BASE_TIME.plusSeconds(1));

        assertEquals(reservation, locked);
        assertEquals(5001L, service.completedMessageId);
        assertEquals("abc", service.completedFingerprint);
    }

    private static final Instant BASE_TIME = Instant.parse("2026-07-21T00:00:00Z");

    private MessageIdempotencyRecord reservation() {
        return new MessageIdempotencyRecord(
                1001L, "message.forward.v1", "forward-1", "abc", null, BASE_TIME, null
        );
    }

    private static final class RecordingService implements MessageIdempotencyDatabaseService {
        private long completedMessageId;
        private String completedFingerprint;

        @Override
        public MessageIdempotencyRecord reserve(MessageIdempotencyRecord reservation) {
            return reservation;
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
            this.completedMessageId = messageId;
            this.completedFingerprint = requestFingerprint;
        }
    }
}

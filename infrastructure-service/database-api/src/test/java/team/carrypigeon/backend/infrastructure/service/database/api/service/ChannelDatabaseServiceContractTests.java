package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChannelDatabaseService 行为契约测试。
 * 职责：锁定 public contract 中默认写入行为的 fail-fast 语义与可覆盖扩展语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class ChannelDatabaseServiceContractTests {

    private static final ChannelRecord RECORD = new ChannelRecord(
            1L,
            1L,
            "general",
            "public",
            true,
            Instant.parse("2026-04-22T00:00:00Z"),
            Instant.parse("2026-04-22T00:00:00Z")
    );

    /**
     * 验证未覆盖默认写入能力时会立即 fail-fast。
     */
    @Test
    @DisplayName("insert default implementation throws unsupported operation")
    void insert_defaultImplementation_throwsUnsupportedOperation() {
        ChannelDatabaseService service = new MinimalChannelDatabaseService();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> service.insert(RECORD)
        );

        assertEquals("channel insert is not supported", exception.getMessage());
    }

    /**
     * 验证实现方可安全覆盖默认写入能力。
     */
    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingChannelDatabaseService service = new RecordingChannelDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    private static class MinimalChannelDatabaseService implements ChannelDatabaseService {

        @Override
        public Optional<ChannelRecord> findDefaultChannel() {
            return Optional.empty();
        }

        @Override
        public Optional<ChannelRecord> findSystemChannel() {
            return Optional.empty();
        }

        @Override
        public Optional<ChannelRecord> findById(long channelId) {
            return Optional.empty();
        }
    }

    private static class RecordingChannelDatabaseService extends MinimalChannelDatabaseService {

        private ChannelRecord insertedRecord;

        @Override
        public void insert(ChannelRecord record) {
            this.insertedRecord = record;
        }
    }
}

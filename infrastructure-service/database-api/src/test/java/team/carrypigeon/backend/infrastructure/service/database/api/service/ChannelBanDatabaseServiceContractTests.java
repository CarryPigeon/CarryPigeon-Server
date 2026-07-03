package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChannelBanDatabaseService 行为契约测试。
 * 职责：锁定频道封禁数据库抽象的默认列表 fallback 语义与读写扩展语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class ChannelBanDatabaseServiceContractTests {

    private static final ChannelBanRecord RECORD = new ChannelBanRecord(
            9L,
            1002L,
            1001L,
            "spam",
            Instant.parse("2026-04-22T00:05:00Z"),
            Instant.parse("2026-04-22T00:00:00Z"),
            null
    );

    /**
     * 验证未覆盖默认列表能力时会返回不可变空列表。
     */
    @Test
    @DisplayName("find by channel id default implementation returns immutable empty list")
    void findByChannelId_defaultImplementation_returnsImmutableEmptyList() {
        ChannelBanDatabaseService service = new MinimalChannelBanDatabaseService();

        List<ChannelBanRecord> records = service.findByChannelId(9L);

        assertTrue(records.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> records.add(RECORD));
    }

    /**
     * 验证实现方可安全覆盖默认列表能力。
     */
    @Test
    @DisplayName("find by channel id overriding implementation returns custom records")
    void findByChannelId_overridingImplementation_returnsCustomRecords() {
        ChannelBanDatabaseService service = new RecordingChannelBanDatabaseService();

        List<ChannelBanRecord> records = service.findByChannelId(9L);

        assertEquals(List.of(RECORD), records);
    }

    /**
     * 验证实现方可接收插入记录。
     */
    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingChannelBanDatabaseService service = new RecordingChannelBanDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    /**
     * 验证实现方可接收更新记录。
     */
    @Test
    @DisplayName("update overriding implementation receives record")
    void update_overridingImplementation_receivesRecord() {
        RecordingChannelBanDatabaseService service = new RecordingChannelBanDatabaseService();

        service.update(RECORD);

        assertSame(RECORD, service.updatedRecord);
    }

    /**
     * `MinimalChannelBanDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class MinimalChannelBanDatabaseService implements ChannelBanDatabaseService {

        @Override
        public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
            return Optional.empty();
        }

        @Override
        public void insert(ChannelBanRecord record) {
        }

        @Override
        public void update(ChannelBanRecord record) {
        }
    }

    /**
     * `RecordingChannelBanDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingChannelBanDatabaseService extends MinimalChannelBanDatabaseService {

        private ChannelBanRecord insertedRecord;
        private ChannelBanRecord updatedRecord;

        @Override
        public List<ChannelBanRecord> findByChannelId(long channelId) {
            return List.of(RECORD);
        }

        @Override
        public void insert(ChannelBanRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void update(ChannelBanRecord record) {
            this.updatedRecord = record;
        }
    }
}

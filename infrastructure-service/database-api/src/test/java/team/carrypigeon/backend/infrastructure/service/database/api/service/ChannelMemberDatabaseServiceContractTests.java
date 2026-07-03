package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChannelMemberDatabaseService 行为契约测试。
 * 职责：锁定 public contract 中默认列表查询行为的静态 fallback 语义与可覆盖扩展语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class ChannelMemberDatabaseServiceContractTests {

    private static final ChannelMemberRecord RECORD = new ChannelMemberRecord(
            1L,
            1001L,
            "OWNER",
            Instant.parse("2026-04-22T00:00:00Z"),
            null
    );

    /**
     * 验证未覆盖默认列表查询能力时会返回不可变空列表。
     */
    @Test
    @DisplayName("find by channel id default implementation returns immutable empty list")
    void findByChannelId_defaultImplementation_returnsImmutableEmptyList() {
        ChannelMemberDatabaseService service = new MinimalChannelMemberDatabaseService();

        List<ChannelMemberRecord> records = service.findByChannelId(1L);

        assertTrue(records.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> records.add(RECORD));
    }

    /**
     * 验证实现方可安全覆盖默认列表查询能力。
     */
    @Test
    @DisplayName("find by channel id overriding implementation returns custom records")
    void findByChannelId_overridingImplementation_returnsCustomRecords() {
        ChannelMemberDatabaseService service = new RecordingChannelMemberDatabaseService();

        List<ChannelMemberRecord> records = service.findByChannelId(1L);

        assertEquals(List.of(RECORD), records);
    }

    /**
     * `MinimalChannelMemberDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class MinimalChannelMemberDatabaseService implements ChannelMemberDatabaseService {

        @Override
        public boolean exists(long channelId, long accountId) {
            return false;
        }

        @Override
        public void insert(ChannelMemberRecord record) {
        }

        @Override
        public Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
            return Optional.empty();
        }

        @Override
        public void update(ChannelMemberRecord record) {
        }

        @Override
        public void delete(long channelId, long accountId) {
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return List.of();
        }
    }

    /**
     * `RecordingChannelMemberDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class RecordingChannelMemberDatabaseService extends MinimalChannelMemberDatabaseService {

        @Override
        public List<ChannelMemberRecord> findByChannelId(long channelId) {
            return List.of(RECORD);
        }
    }
}

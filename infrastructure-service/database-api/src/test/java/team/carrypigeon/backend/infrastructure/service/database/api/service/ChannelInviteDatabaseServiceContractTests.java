package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChannelInviteDatabaseService 行为契约测试。
 * 职责：锁定频道邀请数据库抽象的默认查询 fallback 语义与读写扩展语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class ChannelInviteDatabaseServiceContractTests {

    private static final ChannelInviteRecord RECORD = new ChannelInviteRecord(
            9L,
            3001L,
            1002L,
            1001L,
            "PENDING",
            Instant.parse("2026-04-22T00:00:00Z"),
            null
    );

    /**
     * 验证未覆盖默认按申请 ID 查询能力时会返回空。
     */
    @Test
    @DisplayName("find by channel id and application id default implementation returns empty optional")
    void findByChannelIdAndApplicationId_defaultImplementation_returnsEmptyOptional() {
        ChannelInviteDatabaseService service = new MinimalChannelInviteDatabaseService();

        Optional<ChannelInviteRecord> result = service.findByChannelIdAndApplicationId(9L, 3001L);

        assertTrue(result.isEmpty());
    }

    /**
     * 验证未覆盖默认列表能力时会返回不可变空列表。
     */
    @Test
    @DisplayName("find by channel id default implementation returns immutable empty list")
    void findByChannelId_defaultImplementation_returnsImmutableEmptyList() {
        ChannelInviteDatabaseService service = new MinimalChannelInviteDatabaseService();

        List<ChannelInviteRecord> records = service.findByChannelId(9L);

        assertTrue(records.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> records.add(RECORD));
    }

    /**
     * 验证实现方可安全覆盖按申请 ID 查询能力。
     */
    @Test
    @DisplayName("find by channel id and application id overriding implementation returns record")
    void findByChannelIdAndApplicationId_overridingImplementation_returnsRecord() {
        ChannelInviteDatabaseService service = new RecordingChannelInviteDatabaseService();

        Optional<ChannelInviteRecord> result = service.findByChannelIdAndApplicationId(9L, 3001L);

        assertEquals(Optional.of(RECORD), result);
    }

    /**
     * 验证实现方可安全覆盖列表查询能力。
     */
    @Test
    @DisplayName("find by channel id overriding implementation returns custom records")
    void findByChannelId_overridingImplementation_returnsCustomRecords() {
        ChannelInviteDatabaseService service = new RecordingChannelInviteDatabaseService();

        List<ChannelInviteRecord> records = service.findByChannelId(9L);

        assertEquals(List.of(RECORD), records);
    }

    /**
     * 验证实现方可接收插入记录。
     */
    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingChannelInviteDatabaseService service = new RecordingChannelInviteDatabaseService();

        service.insert(RECORD);

        assertSame(RECORD, service.insertedRecord);
    }

    /**
     * 验证实现方可接收更新记录。
     */
    @Test
    @DisplayName("update overriding implementation receives record")
    void update_overridingImplementation_receivesRecord() {
        RecordingChannelInviteDatabaseService service = new RecordingChannelInviteDatabaseService();

        service.update(RECORD);

        assertSame(RECORD, service.updatedRecord);
    }

    private static class MinimalChannelInviteDatabaseService implements ChannelInviteDatabaseService {

        @Override
        public Optional<ChannelInviteRecord> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
            return Optional.empty();
        }

        @Override
        public void insert(ChannelInviteRecord record) {
        }

        @Override
        public void update(ChannelInviteRecord record) {
        }
    }

    private static final class RecordingChannelInviteDatabaseService extends MinimalChannelInviteDatabaseService {

        private ChannelInviteRecord insertedRecord;
        private ChannelInviteRecord updatedRecord;

        @Override
        public Optional<ChannelInviteRecord> findByChannelIdAndApplicationId(long channelId, long applicationId) {
            return Optional.of(RECORD);
        }

        @Override
        public List<ChannelInviteRecord> findByChannelId(long channelId) {
            return List.of(RECORD);
        }

        @Override
        public void insert(ChannelInviteRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void update(ChannelInviteRecord record) {
            this.updatedRecord = record;
        }
    }
}

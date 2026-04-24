package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseBackedChannelMemberRepository 契约测试。
 * 职责：验证 channel feature 内部成员仓储适配器对角色与禁言字段的映射行为。
 * 边界：不访问真实数据库，只验证领域模型与 database-api 契约模型转换。
 */
@Tag("contract")
class DatabaseBackedChannelMemberRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T11:30:00Z");

    /**
     * 验证查询命中的成员记录会转换成包含角色与禁言信息的领域模型。
     */
    @Test
    @DisplayName("find by channel and account existing record maps governance fields")
    void findByChannelIdAndAccountId_existingRecord_mapsGovernanceFields() {
        FakeChannelMemberDatabaseService databaseService = new FakeChannelMemberDatabaseService();
        databaseService.record = new ChannelMemberRecord(1L, 1001L, "ADMIN", BASE_TIME, BASE_TIME.plusSeconds(300));
        DatabaseBackedChannelMemberRepository repository = new DatabaseBackedChannelMemberRepository(databaseService);

        Optional<ChannelMember> result = repository.findByChannelIdAndAccountId(1L, 1001L);

        assertTrue(result.isPresent());
        assertEquals(ChannelMemberRole.ADMIN, result.orElseThrow().role());
        assertEquals(BASE_TIME.plusSeconds(300), result.orElseThrow().mutedUntil());
    }

    /**
     * 验证保存领域成员时会写入角色与禁言字段。
     */
    @Test
    @DisplayName("save active member writes role and mute fields")
    void save_activeMember_writesRoleAndMuteFields() {
        FakeChannelMemberDatabaseService databaseService = new FakeChannelMemberDatabaseService();
        DatabaseBackedChannelMemberRepository repository = new DatabaseBackedChannelMemberRepository(databaseService);
        ChannelMember channelMember = new ChannelMember(1L, 1001L, ChannelMemberRole.OWNER, BASE_TIME, null);

        repository.save(channelMember);

        assertEquals("OWNER", databaseService.insertedRecord.role());
        assertNull(databaseService.insertedRecord.mutedUntil());
    }

    /**
     * 验证更新领域成员时会写入角色与禁言字段。
     */
    @Test
    @DisplayName("update active member writes role and mute fields")
    void update_activeMember_writesRoleAndMuteFields() {
        FakeChannelMemberDatabaseService databaseService = new FakeChannelMemberDatabaseService();
        DatabaseBackedChannelMemberRepository repository = new DatabaseBackedChannelMemberRepository(databaseService);
        ChannelMember channelMember = new ChannelMember(1L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, BASE_TIME.plusSeconds(600));

        repository.update(channelMember);

        assertEquals("MEMBER", databaseService.updatedRecord.role());
        assertEquals(BASE_TIME.plusSeconds(600), databaseService.updatedRecord.mutedUntil());
    }

    /**
     * 验证删除活跃成员时会下发复合键删除条件。
     */
    @Test
    @DisplayName("delete active member delegates composite key")
    void delete_activeMember_delegatesCompositeKey() {
        FakeChannelMemberDatabaseService databaseService = new FakeChannelMemberDatabaseService();
        DatabaseBackedChannelMemberRepository repository = new DatabaseBackedChannelMemberRepository(databaseService);

        repository.delete(1L, 1002L);

        assertEquals(1L, databaseService.deletedChannelId);
        assertEquals(1002L, databaseService.deletedAccountId);
    }

    /**
     * 验证按频道查询成员列表时会转换为完整领域成员集合。
     */
    @Test
    @DisplayName("find by channel existing records map to domain members")
    void findByChannelId_existingRecords_mapToDomainMembers() {
        FakeChannelMemberDatabaseService databaseService = new FakeChannelMemberDatabaseService();
        databaseService.records = List.of(
                new ChannelMemberRecord(1L, 1001L, "OWNER", BASE_TIME, null),
                new ChannelMemberRecord(1L, 1002L, "MEMBER", BASE_TIME.plusSeconds(60), null)
        );
        DatabaseBackedChannelMemberRepository repository = new DatabaseBackedChannelMemberRepository(databaseService);

        List<ChannelMember> result = repository.findByChannelId(1L);

        assertEquals(2, result.size());
        assertEquals(ChannelMemberRole.OWNER, result.get(0).role());
        assertEquals(1002L, result.get(1).accountId());
    }

    private static class FakeChannelMemberDatabaseService implements ChannelMemberDatabaseService {

        private ChannelMemberRecord record;
        private List<ChannelMemberRecord> records = List.of();
        private ChannelMemberRecord insertedRecord;
        private ChannelMemberRecord updatedRecord;
        private Long deletedChannelId;
        private Long deletedAccountId;

        @Override
        public boolean exists(long channelId, long accountId) {
            return false;
        }

        @Override
        public void insert(ChannelMemberRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public Optional<ChannelMemberRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
            return Optional.ofNullable(record);
        }

        @Override
        public void update(ChannelMemberRecord record) {
            this.updatedRecord = record;
        }

        @Override
        public void delete(long channelId, long accountId) {
            this.deletedChannelId = channelId;
            this.deletedAccountId = accountId;
        }

        @Override
        public List<ChannelMemberRecord> findByChannelId(long channelId) {
            return records;
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return List.of();
        }
    }
}

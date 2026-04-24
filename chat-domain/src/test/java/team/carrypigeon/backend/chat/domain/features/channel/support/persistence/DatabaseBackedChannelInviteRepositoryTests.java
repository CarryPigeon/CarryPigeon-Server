package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInvite;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelInviteStatus;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelInviteRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelInviteDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseBackedChannelInviteRepository 契约测试。
 * 职责：验证 channel feature 内部邀请仓储适配器的领域模型与 database-api 契约模型映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
@Tag("contract")
class DatabaseBackedChannelInviteRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T11:40:00Z");

    /**
     * 验证查询命中的邀请记录会转换成领域模型。
     */
    @Test
    @DisplayName("find by channel and invitee existing record maps to domain model")
    void findByChannelIdAndInviteeAccountId_existingRecord_mapsToDomainModel() {
        FakeChannelInviteDatabaseService databaseService = new FakeChannelInviteDatabaseService();
        databaseService.record = new ChannelInviteRecord(1L, 1002L, 1001L, "PENDING", BASE_TIME, null);
        DatabaseBackedChannelInviteRepository repository = new DatabaseBackedChannelInviteRepository(databaseService);

        Optional<ChannelInvite> result = repository.findByChannelIdAndInviteeAccountId(1L, 1002L);

        assertTrue(result.isPresent());
        assertEquals(ChannelInviteStatus.PENDING, result.orElseThrow().status());
        assertEquals(1001L, result.orElseThrow().inviterAccountId());
    }

    /**
     * 验证更新邀请记录时会写入 database-api 状态字段。
     */
    @Test
    @DisplayName("update domain invite writes database record")
    void update_domainInvite_writesDatabaseRecord() {
        FakeChannelInviteDatabaseService databaseService = new FakeChannelInviteDatabaseService();
        DatabaseBackedChannelInviteRepository repository = new DatabaseBackedChannelInviteRepository(databaseService);
        ChannelInvite channelInvite = new ChannelInvite(
                1L,
                1002L,
                1001L,
                ChannelInviteStatus.ACCEPTED,
                BASE_TIME,
                BASE_TIME.plusSeconds(60)
        );

        repository.update(channelInvite);

        assertEquals("ACCEPTED", databaseService.updatedRecord.status());
        assertEquals(BASE_TIME.plusSeconds(60), databaseService.updatedRecord.respondedAt());
    }

    private static class FakeChannelInviteDatabaseService implements ChannelInviteDatabaseService {

        private ChannelInviteRecord record;
        private ChannelInviteRecord updatedRecord;

        @Override
        public Optional<ChannelInviteRecord> findByChannelIdAndInviteeAccountId(long channelId, long inviteeAccountId) {
            return Optional.ofNullable(record);
        }

        @Override
        public void insert(ChannelInviteRecord record) {
        }

        @Override
        public void update(ChannelInviteRecord record) {
            this.updatedRecord = record;
        }
    }
}

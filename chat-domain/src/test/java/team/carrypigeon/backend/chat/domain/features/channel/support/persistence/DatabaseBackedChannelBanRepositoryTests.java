package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelBan;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelBanRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelBanDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseBackedChannelBanRepository 契约测试。
 * 职责：验证 channel feature 内部封禁仓储适配器的领域模型与 database-api 契约模型映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
@Tag("contract")
class DatabaseBackedChannelBanRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T11:50:00Z");

    /**
     * 验证查询命中的封禁记录会转换成领域模型。
     */
    @Test
    @DisplayName("find by channel and banned account existing record maps to domain model")
    void findByChannelIdAndBannedAccountId_existingRecord_mapsToDomainModel() {
        FakeChannelBanDatabaseService databaseService = new FakeChannelBanDatabaseService();
        databaseService.record = new ChannelBanRecord(1L, 1002L, 1001L, "spam", null, BASE_TIME, null);
        DatabaseBackedChannelBanRepository repository = new DatabaseBackedChannelBanRepository(databaseService);

        Optional<ChannelBan> result = repository.findByChannelIdAndBannedAccountId(1L, 1002L);

        assertTrue(result.isPresent());
        assertEquals("spam", result.orElseThrow().reason());
        assertEquals(1001L, result.orElseThrow().operatorAccountId());
    }

    /**
     * 验证更新封禁记录时会写入解除封禁时间。
     */
    @Test
    @DisplayName("update domain ban writes revoke timestamp")
    void update_domainBan_writesRevokeTimestamp() {
        FakeChannelBanDatabaseService databaseService = new FakeChannelBanDatabaseService();
        DatabaseBackedChannelBanRepository repository = new DatabaseBackedChannelBanRepository(databaseService);
        ChannelBan channelBan = new ChannelBan(1L, 1002L, 1001L, "spam", null, BASE_TIME, BASE_TIME.plusSeconds(120));

        repository.update(channelBan);

        assertEquals(BASE_TIME.plusSeconds(120), databaseService.updatedRecord.revokedAt());
    }

    private static class FakeChannelBanDatabaseService implements ChannelBanDatabaseService {

        private ChannelBanRecord record;
        private ChannelBanRecord updatedRecord;

        @Override
        public Optional<ChannelBanRecord> findByChannelIdAndBannedAccountId(long channelId, long bannedAccountId) {
            return Optional.ofNullable(record);
        }

        @Override
        public void insert(ChannelBanRecord record) {
        }

        @Override
        public void update(ChannelBanRecord record) {
            this.updatedRecord = record;
        }
    }
}

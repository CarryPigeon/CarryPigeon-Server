package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseBackedChannelRepository 契约测试。
 * 职责：验证 channel feature 内部频道仓储适配器的领域模型与 database-api 契约模型映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
@Tag("contract")
class DatabaseBackedChannelRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T11:20:00Z");

    /**
     * 验证按 ID 查询命中的频道记录会转换成领域模型。
     */
    @Test
    @DisplayName("find by id existing record maps to domain model")
    void findById_existingRecord_mapsToDomainModel() {
        FakeChannelDatabaseService databaseService = new FakeChannelDatabaseService();
        databaseService.record = new ChannelRecord(9L, 9L, "project-alpha", "private", false, BASE_TIME, BASE_TIME);
        DatabaseBackedChannelRepository repository = new DatabaseBackedChannelRepository(databaseService);

        Optional<Channel> result = repository.findById(9L);

        assertTrue(result.isPresent());
        assertEquals("project-alpha", result.orElseThrow().name());
        assertEquals("private", result.orElseThrow().type());
    }

    /**
     * 验证保存频道时会写入 database-api 契约记录。
     */
    @Test
    @DisplayName("save domain channel writes database record")
    void save_domainChannel_writesDatabaseRecord() {
        FakeChannelDatabaseService databaseService = new FakeChannelDatabaseService();
        DatabaseBackedChannelRepository repository = new DatabaseBackedChannelRepository(databaseService);
        Channel channel = new Channel(9L, 9L, "project-alpha", "private", false, BASE_TIME, BASE_TIME);

        repository.save(channel);

        assertEquals(9L, databaseService.insertedRecord.id());
        assertEquals(9L, databaseService.insertedRecord.conversationId());
        assertEquals("private", databaseService.insertedRecord.type());
    }

    private static class FakeChannelDatabaseService implements ChannelDatabaseService {

        private ChannelRecord record;
        private ChannelRecord insertedRecord;

        @Override
        public Optional<ChannelRecord> findDefaultChannel() {
            return Optional.empty();
        }

        @Override
        public Optional<ChannelRecord> findById(long channelId) {
            return Optional.ofNullable(record);
        }

        @Override
        public void insert(ChannelRecord record) {
            this.insertedRecord = record;
        }
    }
}

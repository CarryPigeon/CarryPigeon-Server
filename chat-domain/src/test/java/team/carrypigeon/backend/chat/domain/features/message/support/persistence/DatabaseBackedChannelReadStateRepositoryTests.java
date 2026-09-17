package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelReadStateDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 基于 database-api 的频道读状态仓储契约测试。
 * 职责：验证 message 领域模型与 database-api 记录之间的无损映射和委托参数。
 */
@Tag("contract")
class DatabaseBackedChannelReadStateRepositoryTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");

    /**
     * 验证数据库读状态记录会映射为 message 领域模型。
     */
    @Test
    @DisplayName("find read state existing record maps domain model")
    void findByChannelIdAndAccountId_existingRecord_mapsDomainModel() {
        RecordingService service = new RecordingService();
        service.findResult = Optional.of(new ChannelReadStateRecord(
                9L, 1001L, 5001L, BASE_TIME, BASE_TIME.minusSeconds(30), BASE_TIME
        ));
        DatabaseBackedChannelReadStateRepository repository = new DatabaseBackedChannelReadStateRepository(service);

        ChannelReadState result = repository.findByChannelIdAndAccountId(9L, 1001L).orElseThrow();

        assertEquals(9L, service.findChannelId);
        assertEquals(1001L, service.findAccountId);
        assertEquals(5001L, result.lastReadMessageId());
        assertEquals(BASE_TIME.minusSeconds(30), result.createdAt());
    }

    /**
     * 验证写入读状态时完整字段被委托给 database-api，并保持领域对象引用不变。
     */
    @Test
    @DisplayName("upsert read state delegates complete record")
    void upsert_readState_delegatesCompleteRecord() {
        RecordingService service = new RecordingService();
        DatabaseBackedChannelReadStateRepository repository = new DatabaseBackedChannelReadStateRepository(service);
        ChannelReadState readState = new ChannelReadState(
                9L, 1001L, 5001L, BASE_TIME, BASE_TIME.minusSeconds(30), BASE_TIME
        );

        ChannelReadState result = repository.upsert(readState);

        assertSame(readState, result);
        assertEquals(9L, service.upserted.channelId());
        assertEquals(1001L, service.upserted.accountId());
        assertEquals(5001L, service.upserted.lastReadMessageId());
        assertEquals(BASE_TIME, service.upserted.updatedAt());
    }

    /**
     * 验证未读数据库投影会映射为 message 领域未读投影。
     */
    @Test
    @DisplayName("list unreads database records map domain projection")
    void listUnreadsByAccountId_databaseRecords_mapsDomainProjection() {
        RecordingService service = new RecordingService();
        service.unreadResults = List.of(new ChannelUnreadRecord(9L, 3L, BASE_TIME));
        DatabaseBackedChannelReadStateRepository repository = new DatabaseBackedChannelReadStateRepository(service);

        var result = repository.listUnreadsByAccountId(1001L).getFirst();

        assertEquals(1001L, service.unreadAccountId);
        assertEquals(9L, result.channelId());
        assertEquals(3L, result.unreadCount());
        assertEquals(BASE_TIME, result.lastReadTime());
    }

    /**
     * `RecordingService` 测试替身。
     * 职责：记录 database-api 参数并提供固定查询结果。
     */
    private static final class RecordingService implements ChannelReadStateDatabaseService {

        private Optional<ChannelReadStateRecord> findResult = Optional.empty();
        private List<ChannelUnreadRecord> unreadResults = List.of();
        private long findChannelId;
        private long findAccountId;
        private ChannelReadStateRecord upserted;
        private long unreadAccountId;

        @Override
        public Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
            findChannelId = channelId;
            findAccountId = accountId;
            return findResult;
        }

        @Override
        public void upsert(ChannelReadStateRecord record) {
            upserted = record;
        }

        @Override
        public boolean advanceIfNewer(ChannelReadStateRecord record) {
            upserted = record;
            return true;
        }

        @Override
        public List<ChannelUnreadRecord> listUnreadsByAccountId(long accountId) {
            unreadAccountId = accountId;
            return unreadResults;
        }
    }
}

package team.carrypigeon.backend.chat.domain.features.message.support.persistence;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MentionDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DatabaseBackedMentionRepository 契约测试。
 */
@Tag("contract")
class DatabaseBackedMentionRepositoryTests {

    /**
     * 验证 `listByAccountId` 在 `mapsDatabaseRecordsToDomainMentions` 场景下的测试契约。
     */
    @Test
    @DisplayName("list by account id maps database records to domain mentions")
    void listByAccountId_mapsDatabaseRecordsToDomainMentions() {
        RecordingMentionDatabaseService databaseService = new RecordingMentionDatabaseService();
        databaseService.records = List.of(new MentionRecord(11L, 9L, 5001L, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:00:00Z"), false));
        DatabaseBackedMentionRepository repository = new DatabaseBackedMentionRepository(databaseService);

        Mention mention = repository.listByAccountId(1001L, 20L, 10, true, 9L).getFirst();

        assertEquals(1001L, databaseService.accountId);
        assertEquals(20L, databaseService.cursorMentionId);
        assertEquals(10, databaseService.limit);
        assertEquals(true, databaseService.unreadOnly);
        assertEquals(9L, databaseService.channelId);
        assertEquals(11L, mention.mentionId());
        assertEquals(5001L, mention.messageId());
        assertEquals("user", mention.targetType());
    }

    /**
     * 验证 `markAsRead` 在 `delegatesToDatabaseService` 场景下的测试契约。
     */
    @Test
    @DisplayName("mark as read delegates to database service")
    void markAsRead_delegatesToDatabaseService() {
        RecordingMentionDatabaseService databaseService = new RecordingMentionDatabaseService();
        databaseService.markAsReadResult = true;
        DatabaseBackedMentionRepository repository = new DatabaseBackedMentionRepository(databaseService);

        boolean result = repository.markAsRead(1001L, 11L);

        assertEquals(true, result);
        assertEquals(1001L, databaseService.markAsReadAccountId);
        assertEquals(11L, databaseService.markAsReadMentionId);
    }

    /**
     * 验证 `save` 在 `delegatesToDatabaseService` 场景下的测试契约。
     */
    @Test
    @DisplayName("save delegates to database service")
    void save_delegatesToDatabaseService() {
        RecordingMentionDatabaseService databaseService = new RecordingMentionDatabaseService();
        DatabaseBackedMentionRepository repository = new DatabaseBackedMentionRepository(databaseService);

        repository.save(new Mention(12L, 9L, 5002L, 1002L, "user", 1001L, Instant.parse("2026-04-24T12:01:00Z"), false));

        assertEquals(12L, databaseService.insertedRecord.mentionId());
        assertEquals(5002L, databaseService.insertedRecord.messageId());
    }

    /**
     * 验证 `markAllAsRead` 在 `delegatesToDatabaseService` 场景下的测试契约。
     */
    @Test
    @DisplayName("mark all as read delegates to database service")
    void markAllAsRead_delegatesToDatabaseService() {
        RecordingMentionDatabaseService databaseService = new RecordingMentionDatabaseService();
        databaseService.markAllAsReadResult = 3;
        DatabaseBackedMentionRepository repository = new DatabaseBackedMentionRepository(databaseService);

        int result = repository.markAllAsRead(1001L, 88L, 9L);

        assertEquals(3, result);
        assertEquals(1001L, databaseService.markAllAccountId);
        assertEquals(88L, databaseService.markAllBeforeMentionId);
        assertEquals(9L, databaseService.markAllChannelId);
    }

    /**
     * `RecordingMentionDatabaseService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static final class RecordingMentionDatabaseService implements MentionDatabaseService {
        private long accountId;
        private Long cursorMentionId;
        private int limit;
        private boolean unreadOnly;
        private Long channelId;
        private List<MentionRecord> records = List.of();
        private MentionRecord insertedRecord;
        private boolean markAsReadResult;
        private long markAsReadAccountId;
        private long markAsReadMentionId;
        private int markAllAsReadResult;
        private long markAllAccountId;
        private Long markAllBeforeMentionId;
        private Long markAllChannelId;

        @Override
        public void insert(MentionRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
            this.accountId = accountId;
            this.cursorMentionId = cursorMentionId;
            this.limit = limit;
            this.unreadOnly = unreadOnly;
            this.channelId = channelId;
            return records;
        }

        @Override
        public boolean markAsRead(long accountId, long mentionId) {
            this.markAsReadAccountId = accountId;
            this.markAsReadMentionId = mentionId;
            return markAsReadResult;
        }

        @Override
        public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
            this.markAllAccountId = accountId;
            this.markAllBeforeMentionId = beforeMentionId;
            this.markAllChannelId = channelId;
            return markAllAsReadResult;
        }
    }
}

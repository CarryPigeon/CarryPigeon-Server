package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MentionRecord;

import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("contract")
class MentionDatabaseServiceContractTests {

    @Test
    @DisplayName("list by account overriding implementation returns records")
    void listByAccount_overridingImplementation_returnsRecords() {
        MentionRecord record = new MentionRecord(1L, 9L, 5001L, 1002L, "user", 1001L, java.time.Instant.parse("2026-04-24T12:00:00Z"), false);
        MentionDatabaseService service = new MentionDatabaseService() {
            @Override
            public void insert(MentionRecord record) {
            }

            @Override
            public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
                return List.of(record);
            }

            @Override
            public boolean markAsRead(long accountId, long mentionId) {
                return true;
            }

            @Override
            public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
                return 1;
            }
        };

        assertSame(record, service.listByAccountId(1001L, null, 20, false, null).getFirst());
    }

    @Test
    @DisplayName("mark mention read overriding implementation returns boolean")
    void markMentionRead_overridingImplementation_returnsBoolean() {
        MentionDatabaseService service = new MentionDatabaseService() {
            @Override
            public void insert(MentionRecord record) {
            }

            @Override
            public List<MentionRecord> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
                return List.of();
            }

            @Override
            public boolean markAsRead(long accountId, long mentionId) {
                return true;
            }

            @Override
            public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
                return 0;
            }
        };

        org.junit.jupiter.api.Assertions.assertEquals(true, service.markAsRead(1001L, 11L));
    }
}

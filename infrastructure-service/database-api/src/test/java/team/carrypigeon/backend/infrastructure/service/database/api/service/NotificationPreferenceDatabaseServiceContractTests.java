package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationChannelPreferenceRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.NotificationServerPreferenceRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
/**
 * NotificationPreferenceDatabaseService 契约测试。
 * 职责：锁定服务端级与频道级通知偏好的数据库抽象输入输出语义。
 * 边界：不验证具体数据库实现，只验证调用方与实现方之间的最小记录契约。
 */
@Tag("contract")
class NotificationPreferenceDatabaseServiceContractTests {

    /**
     * 验证查询服务端级偏好会按账户 ID 返回稳定记录。
     */
    @Test
    @DisplayName("find server preference existing account returns record")
    void findServerPreferenceByAccountId_existingAccount_returnsRecord() {
        NotificationServerPreferenceRecord serverRecord = new NotificationServerPreferenceRecord(1001L, "all", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        RecordingNotificationPreferenceDatabaseService service = new RecordingNotificationPreferenceDatabaseService();
        service.serverRecord = serverRecord;

        NotificationServerPreferenceRecord result = service.findServerPreferenceByAccountId(1001L).orElseThrow();

        assertSame(serverRecord, result);
        assertEquals(1001L, service.lastServerAccountId);
    }

    /**
     * 验证查询频道级偏好会按账户 ID 返回频道偏好列表。
     */
    @Test
    @DisplayName("list channel preferences account returns records")
    void listChannelPreferencesByAccountId_account_returnsRecords() {
        NotificationChannelPreferenceRecord channelRecord = new NotificationChannelPreferenceRecord(1001L, 9L, "inherit", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        RecordingNotificationPreferenceDatabaseService service = new RecordingNotificationPreferenceDatabaseService();
        service.channelRecords = List.of(channelRecord);

        NotificationChannelPreferenceRecord result = service.listChannelPreferencesByAccountId(1001L).getFirst();

        assertEquals(1001L, service.lastChannelListAccountId);
        assertEquals(9L, result.channelId());
        assertEquals("inherit", result.mode());
    }

    /**
     * 验证写入偏好时会把服务端级与频道级记录原样传递给实现方。
     */
    @Test
    @DisplayName("upsert preferences records passes complete records")
    void upsertPreferences_records_passesCompleteRecords() {
        NotificationServerPreferenceRecord serverRecord = new NotificationServerPreferenceRecord(1001L, "all", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        NotificationChannelPreferenceRecord channelRecord = new NotificationChannelPreferenceRecord(1001L, 9L, "inherit", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        RecordingNotificationPreferenceDatabaseService service = new RecordingNotificationPreferenceDatabaseService();

        service.upsertServerPreference(serverRecord);
        service.upsertChannelPreference(channelRecord);

        assertSame(serverRecord, service.updatedServerRecord);
        assertSame(channelRecord, service.updatedChannelRecord);
    }

    /**
     * RecordingNotificationPreferenceDatabaseService 测试替身。
     * 职责：记录接口调用参数，使测试只验证 database-api 抽象契约。
     */
    private static class RecordingNotificationPreferenceDatabaseService implements NotificationPreferenceDatabaseService {
        private NotificationServerPreferenceRecord serverRecord;
        private List<NotificationChannelPreferenceRecord> channelRecords = List.of();
        private NotificationServerPreferenceRecord updatedServerRecord;
        private NotificationChannelPreferenceRecord updatedChannelRecord;
        private long lastServerAccountId;
        private long lastChannelListAccountId;

        @Override
        public Optional<NotificationServerPreferenceRecord> findServerPreferenceByAccountId(long accountId) {
            this.lastServerAccountId = accountId;
            return Optional.ofNullable(serverRecord);
        }

        @Override
        public List<NotificationChannelPreferenceRecord> listChannelPreferencesByAccountId(long accountId) {
            this.lastChannelListAccountId = accountId;
            return channelRecords;
        }

        @Override
        public void upsertServerPreference(NotificationServerPreferenceRecord record) {
            this.updatedServerRecord = record;
        }

        @Override
        public void upsertChannelPreference(NotificationChannelPreferenceRecord record) {
            this.updatedChannelRecord = record;
        }
    }
}

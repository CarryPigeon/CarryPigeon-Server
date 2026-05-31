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

@Tag("contract")
class NotificationPreferenceDatabaseServiceContractTests {

    @Test
    @DisplayName("find and list preference overriding implementation returns records")
    void findAndListPreference_overridingImplementation_returnsRecords() {
        NotificationServerPreferenceRecord serverRecord = new NotificationServerPreferenceRecord(1001L, "all", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        NotificationChannelPreferenceRecord channelRecord = new NotificationChannelPreferenceRecord(1001L, 9L, "inherit", 0L, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z"));
        NotificationPreferenceDatabaseService service = new NotificationPreferenceDatabaseService() {
            @Override public Optional<NotificationServerPreferenceRecord> findServerPreferenceByAccountId(long accountId) { return Optional.of(serverRecord); }
            @Override public List<NotificationChannelPreferenceRecord> listChannelPreferencesByAccountId(long accountId) { return List.of(channelRecord); }
            @Override public void upsertServerPreference(NotificationServerPreferenceRecord record) { }
            @Override public void upsertChannelPreference(NotificationChannelPreferenceRecord record) { }
        };

        assertEquals("all", service.findServerPreferenceByAccountId(1001L).orElseThrow().mode());
        assertEquals(9L, service.listChannelPreferencesByAccountId(1001L).getFirst().channelId());
    }
}

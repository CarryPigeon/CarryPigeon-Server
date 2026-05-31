package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelUnreadRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("contract")
class ChannelReadStateDatabaseServiceContractTests {

    private static final ChannelReadStateRecord RECORD = new ChannelReadStateRecord(1L, 1001L, 5001L, Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"));

    @Test
    @DisplayName("upsert default implementation throws unsupported operation")
    void upsert_defaultImplementation_throwsUnsupportedOperation() {
        ChannelReadStateDatabaseService service = new MinimalChannelReadStateDatabaseService();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> service.upsert(RECORD)
        );

        assertEquals("channel read state upsert is not supported", exception.getMessage());
    }

    @Test
    @DisplayName("upsert overriding implementation receives record")
    void upsert_overridingImplementation_receivesRecord() {
        RecordingChannelReadStateDatabaseService service = new RecordingChannelReadStateDatabaseService();

        service.upsert(RECORD);

        assertSame(RECORD, service.updatedRecord);
    }

    @Test
    @DisplayName("list unreads default implementation throws unsupported operation")
    void listUnreads_defaultImplementation_throwsUnsupportedOperation() {
        ChannelReadStateDatabaseService service = new MinimalChannelReadStateDatabaseService();

        assertThrows(UnsupportedOperationException.class, () -> service.listUnreadsByAccountId(1001L));
    }

    private static class MinimalChannelReadStateDatabaseService implements ChannelReadStateDatabaseService {
        @Override
        public Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
            return Optional.empty();
        }

        @Override
        public void upsert(ChannelReadStateRecord record) {
            throw new UnsupportedOperationException("channel read state upsert is not supported");
        }

        @Override
        public List<ChannelUnreadRecord> listUnreadsByAccountId(long accountId) {
            throw new UnsupportedOperationException("channel unread list is not supported");
        }
    }

    private static class RecordingChannelReadStateDatabaseService extends MinimalChannelReadStateDatabaseService {
        private ChannelReadStateRecord updatedRecord;

        @Override
        public void upsert(ChannelReadStateRecord record) {
            this.updatedRecord = record;
        }
    }
}

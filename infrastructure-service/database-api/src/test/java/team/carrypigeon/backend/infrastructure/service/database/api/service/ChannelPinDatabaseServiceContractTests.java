package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelPinRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("contract")
class ChannelPinDatabaseServiceContractTests {

    private static final ChannelPinRecord RECORD = new ChannelPinRecord(7001L, 1L, 5001L, 1001L, "note", Instant.parse("2026-04-22T00:00:00Z"));

    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingChannelPinDatabaseService service = new RecordingChannelPinDatabaseService();
        service.insert(RECORD);
        assertSame(RECORD, service.insertedRecord);
    }

    private static class RecordingChannelPinDatabaseService implements ChannelPinDatabaseService {
        private ChannelPinRecord insertedRecord;

        @Override
        public Optional<ChannelPinRecord> findByChannelIdAndMessageId(long channelId, long messageId) {
            return Optional.empty();
        }

        @Override
        public void insert(ChannelPinRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public void delete(long channelId, long messageId) {
        }

        @Override
        public List<ChannelPinRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return List.of();
        }

        @Override
        public long countByChannelId(long channelId) {
            return 0;
        }
    }
}

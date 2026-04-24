package team.carrypigeon.backend.chat.domain.features.channel.support.persistence;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelAuditLogDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DatabaseBackedChannelAuditLogRepository 契约测试。
 * 职责：验证 channel feature 内部审计仓储适配器的追加式写入映射行为。
 * 边界：不访问真实数据库，只验证运行时适配转换。
 */
@Tag("contract")
class DatabaseBackedChannelAuditLogRepositoryTests {

    /**
     * 验证追加审计记录时会写入最小审计字段。
     */
    @Test
    @DisplayName("append audit log writes database record")
    void append_auditLog_writesDatabaseRecord() {
        FakeChannelAuditLogDatabaseService databaseService = new FakeChannelAuditLogDatabaseService();
        DatabaseBackedChannelAuditLogRepository repository = new DatabaseBackedChannelAuditLogRepository(databaseService);
        ChannelAuditLog channelAuditLog = new ChannelAuditLog(
                7001L,
                1L,
                1001L,
                "MEMBER_MUTED",
                1002L,
                "{\"durationSeconds\":300}",
                Instant.parse("2026-04-24T12:00:00Z")
        );

        repository.append(channelAuditLog);

        assertEquals(7001L, databaseService.insertedRecord.auditId());
        assertEquals("MEMBER_MUTED", databaseService.insertedRecord.actionType());
        assertEquals("{\"durationSeconds\":300}", databaseService.insertedRecord.metadata());
    }

    private static class FakeChannelAuditLogDatabaseService implements ChannelAuditLogDatabaseService {

        private ChannelAuditLogRecord insertedRecord;

        @Override
        public void insert(ChannelAuditLogRecord record) {
            this.insertedRecord = record;
        }
    }
}

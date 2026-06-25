package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogReadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelAuditLogWriteRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChannelAuditLogDatabaseService 行为契约测试。
 * 职责：锁定频道审计日志数据库抽象的默认列表 fallback 语义与追加写入扩展语义。
 * 边界：只验证接口默认行为，不依赖具体数据库实现。
 */
@Tag("contract")
class ChannelAuditLogDatabaseServiceContractTests {

    private static final ChannelAuditLogWriteRecord WRITE_RECORD = new ChannelAuditLogWriteRecord(
            8001L,
            9L,
            1001L,
            "MEMBER_BANNED",
            1002L,
            "{\"reason\":\"spam\"}",
            Instant.parse("2026-04-22T00:00:00Z")
    );

    private static final ChannelAuditLogReadRecord READ_RECORD = new ChannelAuditLogReadRecord(
            8001L,
            9L,
            1001L,
            "MEMBER_BANNED",
            "{\"reason\":\"spam\"}",
            Instant.parse("2026-04-22T00:00:00Z")
    );

    /**
     * 验证未覆盖默认 list 能力时会立即 fail-fast。
     */
    @Test
    @DisplayName("list default implementation throws unsupported operation")
    void list_defaultImplementation_throwsUnsupportedOperation() {
        ChannelAuditLogDatabaseService service = new MinimalChannelAuditLogDatabaseService();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> service.list(null, 20, 9L, null, null, null, null)
        );

        assertEquals("channel audit log list is not supported", exception.getMessage());
    }

    /**
     * 验证实现方可安全覆盖默认列表查询能力。
     */
    @Test
    @DisplayName("list overriding implementation returns custom records")
    void list_overridingImplementation_returnsCustomRecords() {
        ChannelAuditLogDatabaseService service = new RecordingChannelAuditLogDatabaseService();

        List<ChannelAuditLogReadRecord> result = service.list(null, 20, 9L, null, null, null, null);

        assertEquals(List.of(READ_RECORD), result);
    }

    /**
     * 验证实现方可接收追加写入记录。
     */
    @Test
    @DisplayName("insert overriding implementation receives record")
    void insert_overridingImplementation_receivesRecord() {
        RecordingChannelAuditLogDatabaseService service = new RecordingChannelAuditLogDatabaseService();

        service.insert(WRITE_RECORD);

        assertSame(WRITE_RECORD, service.insertedRecord);
    }

    private static class MinimalChannelAuditLogDatabaseService implements ChannelAuditLogDatabaseService {

        @Override
        public void insert(ChannelAuditLogWriteRecord record) {
        }
    }

    private static final class RecordingChannelAuditLogDatabaseService extends MinimalChannelAuditLogDatabaseService {

        private ChannelAuditLogWriteRecord insertedRecord;

        @Override
        public void insert(ChannelAuditLogWriteRecord record) {
            this.insertedRecord = record;
        }

        @Override
        public List<ChannelAuditLogReadRecord> list(
                Long cursorAuditId,
                int limit,
                Long channelId,
                Long actorAccountId,
                String actionType,
                Instant fromTime,
                Instant toTime
        ) {
            return List.of(READ_RECORD);
        }
    }
}

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
/**
 * ChannelReadStateDatabaseService 契约测试。
 * 职责：锁定频道已读状态数据库抽象的最小读写输入输出语义。
 * 边界：不验证具体数据库实现，只验证上层可依赖的接口参数与返回模型。
 */
@Tag("contract")
class ChannelReadStateDatabaseServiceContractTests {

    private static final ChannelReadStateRecord RECORD = new ChannelReadStateRecord(1L, 1001L, 5001L, Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z"));

    /**
     * 验证按频道和账户查询会返回对应已读状态记录。
     */
    @Test
    @DisplayName("find by channel and account existing state returns record")
    void findByChannelIdAndAccountId_existingState_returnsRecord() {
        RecordingChannelReadStateDatabaseService service = new RecordingChannelReadStateDatabaseService();
        service.updatedRecord = RECORD;

        ChannelReadStateRecord result = service.findByChannelIdAndAccountId(1L, 1001L).orElseThrow();

        assertSame(RECORD, result);
        assertEquals(1L, service.lastChannelId);
        assertEquals(1001L, service.lastAccountId);
    }

    /**
     * 验证写入已读状态时会把完整记录传递给实现方。
     */
    @Test
    @DisplayName("upsert record passes complete read state")
    void upsert_record_passesCompleteReadState() {
        RecordingChannelReadStateDatabaseService service = new RecordingChannelReadStateDatabaseService();

        service.upsert(RECORD);

        assertSame(RECORD, service.updatedRecord);
    }

    /**
     * 验证未读列表查询会按账户 ID 返回未读投影。
     */
    @Test
    @DisplayName("list unreads account returns unread projections")
    void listUnreadsByAccountId_account_returnsUnreadProjections() {
        RecordingChannelReadStateDatabaseService service = new RecordingChannelReadStateDatabaseService();

        ChannelUnreadRecord result = service.listUnreadsByAccountId(1001L).getFirst();

        assertEquals(1001L, service.lastUnreadAccountId);
        assertEquals(1L, result.channelId());
        assertEquals(9L, result.unreadCount());
    }

    /**
     * RecordingChannelReadStateDatabaseService 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class RecordingChannelReadStateDatabaseService implements ChannelReadStateDatabaseService {
        private ChannelReadStateRecord updatedRecord;
        private long lastChannelId;
        private long lastAccountId;
        private long lastUnreadAccountId;

        @Override
        public Optional<ChannelReadStateRecord> findByChannelIdAndAccountId(long channelId, long accountId) {
            this.lastChannelId = channelId;
            this.lastAccountId = accountId;
            return Optional.ofNullable(updatedRecord);
        }

        @Override
        public void upsert(ChannelReadStateRecord record) {
            this.updatedRecord = record;
        }

        @Override
        public List<ChannelUnreadRecord> listUnreadsByAccountId(long accountId) {
            this.lastUnreadAccountId = accountId;
            return List.of(new ChannelUnreadRecord(1L, 9L, Instant.parse("2026-04-22T00:00:00Z")));
        }
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageEventRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageEventPageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageEventDatabaseService;

/** MySQL 消息变更流；序号行锁保证同频道事件的分配顺序与提交可见顺序一致。 */
@Transactional(propagation = Propagation.MANDATORY)
public class JdbcMessageEventDatabaseService implements MessageEventDatabaseService {
    private static final RowMapper<MessageEventRecord> EVENT = (row, index) -> toRecord(row);
    private final JdbcClient jdbc;

    public JdbcMessageEventDatabaseService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(long eventId, long channelId, long messageId, String type, String payload) {
        jdbc.sql("""
                INSERT INTO chat_message_event_stream(channel_id, last_sequence, expired_through) VALUES (:cid, 1, 0)
                ON DUPLICATE KEY UPDATE last_sequence = last_sequence + 1
                """).param("cid", channelId).update();
        long sequence = jdbc.sql("SELECT last_sequence FROM chat_message_event_stream WHERE channel_id=:cid FOR UPDATE")
                .param("cid", channelId).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO chat_message_event(event_id, channel_id, message_id, event_sequence, event_type, payload)
                VALUES (:id, :cid, :mid, :sequence, :type, :payload)
                """).param("id", eventId).param("cid", channelId).param("sequence", sequence)
                .param("mid", messageId).param("type", type).param("payload", payload).update();
    }

    @Override
    public void redactCreated(long channelId, long messageId, String replacementPayload) {
        jdbc.sql("""
                UPDATE chat_message_event SET payload=:payload
                WHERE channel_id=:cid AND message_id=:mid AND event_type='message.created'
                """).param("payload", replacementPayload).param("cid", channelId).param("mid", messageId).update();
    }

    @Override
    public MessageEventPageRecord readChanges(long channelId, Long afterSequence, int limit) {
        var state = jdbc.sql("SELECT last_sequence, expired_through FROM chat_message_event_stream WHERE channel_id=:cid FOR SHARE")
                .param("cid", channelId).query((row, index) -> new long[]{row.getLong(1), row.getLong(2)}).optional();
        if (state.isEmpty()) {
            return new MessageEventPageRecord(0, 0, List.of());
        }
        long[] bounds = state.get();
        if (afterSequence == null || afterSequence < bounds[1] || afterSequence > bounds[0]) {
            return new MessageEventPageRecord(bounds[0], bounds[1], List.of());
        }
        // 当前读与上面的边界锁配合，避免复用授权阶段更早的 REPEATABLE READ 快照。
        var events = jdbc.sql("""
                SELECT *, UNIX_TIMESTAMP(created_at)*1000 AS created_at_millis FROM chat_message_event WHERE channel_id=:cid AND event_sequence>:after
                ORDER BY event_sequence LIMIT :limit FOR SHARE
                """).param("cid", channelId).param("after", afterSequence).param("limit", limit).query(EVENT).list();
        return new MessageEventPageRecord(bounds[0], bounds[1], events);
    }

    @Override
    public Optional<MessageEventRecord> claimPending(Instant cutoff) {
        // 必须遍历未派发状态；较小 ID 或序号的事务可能在上一批查询之后才提交。
        return jdbc.sql("""
                SELECT *, UNIX_TIMESTAMP(created_at)*1000 AS created_at_millis FROM chat_message_event WHERE delivered_at IS NULL
                AND next_attempt_at<=CURRENT_TIMESTAMP(3) AND created_at>=FROM_UNIXTIME(:cutoff)
                ORDER BY next_attempt_at, event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """).param("cutoff", BigDecimal.valueOf(cutoff.toEpochMilli(), 3)).query(EVENT).optional();
    }

    @Override
    public void markDelivered(long eventId) {
        jdbc.sql("UPDATE chat_message_event SET delivered_at=CURRENT_TIMESTAMP(3) WHERE event_id=:id")
                .param("id", eventId).update();
    }

    @Override
    public void retryLater(long eventId) {
        jdbc.sql("""
                UPDATE chat_message_event SET next_attempt_at=TIMESTAMPADD(SECOND,
                    LEAST(60, POW(2, LEAST(attempts, 6))), CURRENT_TIMESTAMP(3)), attempts=attempts+1 WHERE event_id=:id
                """).param("id", eventId).update();
    }

    @Override
    public int purgeExpired(Instant cutoff, int limit) {
        var channel = jdbc.sql("""
                SELECT e.channel_id FROM chat_message_event e JOIN chat_message_event_stream s
                ON s.channel_id=e.channel_id AND e.event_sequence=s.expired_through+1
                WHERE e.created_at<FROM_UNIXTIME(:cutoff) ORDER BY e.created_at LIMIT 1
                """).param("cutoff", BigDecimal.valueOf(cutoff.toEpochMilli(), 3)).query(Long.class).optional();
        if (channel.isEmpty()) {
            return 0;
        }
        long cid = channel.get();
        var locked = jdbc.sql("SELECT channel_id FROM chat_message_event_stream WHERE channel_id=:cid FOR UPDATE SKIP LOCKED")
                .param("cid", cid).query(Long.class).optional();
        if (locked.isEmpty()) {
            return 0;
        }
        var prefix = jdbc.sql("SELECT *, UNIX_TIMESTAMP(created_at)*1000 AS created_at_millis FROM chat_message_event WHERE channel_id=:cid ORDER BY event_sequence LIMIT :limit FOR UPDATE")
                .param("cid", cid).param("limit", limit).query(EVENT).list().stream()
                .takeWhile(event -> event.createdAt().isBefore(cutoff)).toList();
        if (prefix.isEmpty()) {
            return 0;
        }
        long through = prefix.getLast().sequence();
        int removed = jdbc.sql("DELETE FROM chat_message_event WHERE channel_id=:cid AND event_sequence<=:through")
                .param("cid", cid).param("through", through).update();
        // 即使整段事件已清空也保留流边界，旧游标必须得到过期响应而不是空成功。
        jdbc.sql("UPDATE chat_message_event_stream SET expired_through=:through WHERE channel_id=:cid")
                .param("through", through).param("cid", cid).update();
        return removed;
    }

    @Override
    public boolean deleteChannelBatch(long channelId, int limit) {
        int removed = jdbc.sql("DELETE FROM chat_message_event WHERE channel_id=:cid LIMIT :limit")
                .param("cid", channelId).param("limit", limit).update();
        if (removed != 0) {
            return false;
        }
        jdbc.sql("DELETE FROM chat_message_event_stream WHERE channel_id=:cid").param("cid", channelId).update();
        return true;
    }

    private static MessageEventRecord toRecord(ResultSet row) throws SQLException {
        // 数据库默认生成的时间按 epoch 读取，避免 JDBC 声明时区与会话时区不一致时偏移恢复窗口。
        return new MessageEventRecord(row.getLong("event_id"), row.getLong("channel_id"), row.getLong("event_sequence"),
                row.getString("event_type"), row.getString("payload"), Instant.ofEpochMilli(row.getLong("created_at_millis")));
    }
}

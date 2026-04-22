package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;

/**
 * JDBC 消息数据库服务。
 * 职责：在 database-impl 中完成消息最小读写能力。
 * 边界：只负责数据库记录映射，不承载消息业务规则。
 */
public class JdbcMessageDatabaseService implements MessageDatabaseService {

    private static final String INSERT_SQL = """
            INSERT INTO chat_message (
                message_id,
                server_id,
                conversation_id,
                channel_id,
                sender_id,
                message_type,
                content,
                payload,
                metadata,
                status,
                created_at
            )
            VALUES (
                :messageId,
                :serverId,
                :conversationId,
                :channelId,
                :senderId,
                :messageType,
                :content,
                :payload,
                :metadata,
                :status,
                :createdAt
            )
            """;

    private static final String FIND_BY_CHANNEL_SQL = """
            SELECT message_id, server_id, conversation_id, channel_id, sender_id, message_type,
                   content, payload, metadata, status, created_at
            FROM chat_message
            WHERE channel_id = :channelId
            ORDER BY message_id DESC
            LIMIT :limit
            """;

    private static final String FIND_BY_CHANNEL_WITH_CURSOR_SQL = """
            SELECT message_id, server_id, conversation_id, channel_id, sender_id, message_type,
                   content, payload, metadata, status, created_at
            FROM chat_message
            WHERE channel_id = :channelId AND message_id < :cursorMessageId
            ORDER BY message_id DESC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public JdbcMessageDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void insert(MessageRecord record) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("messageId", record.messageId())
                    .param("serverId", record.serverId())
                    .param("conversationId", record.conversationId())
                    .param("channelId", record.channelId())
                    .param("senderId", record.senderId())
                    .param("messageType", record.messageType())
                    .param("content", record.content())
                    .param("payload", record.payload())
                    .param("metadata", record.metadata())
                    .param("status", record.status())
                    .param("createdAt", Timestamp.from(record.createdAt()))
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert message", exception);
        }
    }

    @Override
    public List<MessageRecord> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
        try {
            JdbcClient.StatementSpec statementSpec = jdbcClient.sql(
                    cursorMessageId == null ? FIND_BY_CHANNEL_SQL : FIND_BY_CHANNEL_WITH_CURSOR_SQL
            ).param("channelId", channelId)
                    .param("limit", limit);
            if (cursorMessageId != null) {
                statementSpec = statementSpec.param("cursorMessageId", cursorMessageId);
            }
            return statementSpec.query((rs, rowNum) -> new MessageRecord(
                    rs.getLong("message_id"),
                    rs.getString("server_id"),
                    rs.getLong("conversation_id"),
                    rs.getLong("channel_id"),
                    rs.getLong("sender_id"),
                    rs.getString("message_type"),
                    rs.getString("content"),
                    rs.getString("payload"),
                    rs.getString("metadata"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant()
            )).list();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel messages", exception);
        }
    }
}

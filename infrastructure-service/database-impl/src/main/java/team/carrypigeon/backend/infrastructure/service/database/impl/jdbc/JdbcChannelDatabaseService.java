package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelDatabaseService;

/**
 * JDBC 频道数据库服务。
 * 职责：在 database-impl 中完成频道最小查询能力。
 * 边界：只负责数据库记录映射，不承载频道业务规则。
 */
public class JdbcChannelDatabaseService implements ChannelDatabaseService {

    private static final String FIND_DEFAULT_SQL = """
            SELECT id, conversation_id, name, type, is_default, created_at, updated_at
            FROM chat_channel
            WHERE is_default = true AND type = 'public'
            LIMIT 1
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, conversation_id, name, type, is_default, created_at, updated_at
            FROM chat_channel
            WHERE id = :channelId
            """;

    private final JdbcClient jdbcClient;

    public JdbcChannelDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<ChannelRecord> findDefaultChannel() {
        try {
            return jdbcClient.sql(FIND_DEFAULT_SQL)
                    .query((rs, rowNum) -> new ChannelRecord(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getBoolean("is_default"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant()
                    ))
                    .optional();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query default channel", exception);
        }
    }

    @Override
    public Optional<ChannelRecord> findById(long channelId) {
        try {
            return jdbcClient.sql(FIND_BY_ID_SQL)
                    .param("channelId", channelId)
                    .query((rs, rowNum) -> new ChannelRecord(
                            rs.getLong("id"),
                            rs.getLong("conversation_id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getBoolean("is_default"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant()
                    ))
                    .optional();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel by id", exception);
        }
    }
}

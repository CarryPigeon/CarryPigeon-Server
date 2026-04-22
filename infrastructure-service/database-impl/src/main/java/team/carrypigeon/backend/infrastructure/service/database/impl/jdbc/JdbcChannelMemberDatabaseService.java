package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelMemberRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.ChannelMemberDatabaseService;

/**
 * JDBC 频道成员数据库服务。
 * 职责：在 database-impl 中完成频道成员最小读写能力。
 * 边界：只负责数据库记录映射，不承载成员业务规则。
 */
public class JdbcChannelMemberDatabaseService implements ChannelMemberDatabaseService {

    private static final String EXISTS_SQL = """
            SELECT COUNT(1)
            FROM chat_channel_member
            WHERE channel_id = :channelId AND account_id = :accountId
            """;

    private static final String INSERT_SQL = """
            INSERT INTO chat_channel_member (channel_id, account_id, joined_at)
            VALUES (:channelId, :accountId, :joinedAt)
            """;

    private static final String FIND_ACCOUNT_IDS_SQL = """
            SELECT account_id
            FROM chat_channel_member
            WHERE channel_id = :channelId
            ORDER BY account_id ASC
            """;

    private final JdbcClient jdbcClient;

    public JdbcChannelMemberDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean exists(long channelId, long accountId) {
        try {
            Long count = jdbcClient.sql(EXISTS_SQL)
                    .param("channelId", channelId)
                    .param("accountId", accountId)
                    .query(Long.class)
                    .single();
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel membership", exception);
        }
    }

    @Override
    public void insert(ChannelMemberRecord record) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("channelId", record.channelId())
                    .param("accountId", record.accountId())
                    .param("joinedAt", Timestamp.from(record.joinedAt()))
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert channel membership", exception);
        }
    }

    @Override
    public List<Long> findAccountIdsByChannelId(long channelId) {
        try {
            return jdbcClient.sql(FIND_ACCOUNT_IDS_SQL)
                    .param("channelId", channelId)
                    .query(Long.class)
                    .list();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query channel member account ids", exception);
        }
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.UserProfileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.UserProfileDatabaseService;

/**
 * JDBC 用户资料数据库服务。
 * 职责：在 database-impl 中完成用户资料的最小查询与更新。
 * 边界：只负责数据库记录映射，不承载用户资料业务规则与协议决策。
 */
public class JdbcUserProfileDatabaseService implements UserProfileDatabaseService {

    private static final String FIND_BY_ACCOUNT_ID_SQL = """
            SELECT account_id, nickname, avatar_url, bio, created_at, updated_at
            FROM user_profile
            WHERE account_id = :accountId
            """;

    private static final String UPDATE_SQL = """
            UPDATE user_profile
            SET nickname = :nickname,
                avatar_url = :avatarUrl,
                bio = :bio,
                updated_at = :updatedAt
            WHERE account_id = :accountId
            """;

    private static final String INSERT_SQL = """
            INSERT INTO user_profile (account_id, nickname, avatar_url, bio, created_at, updated_at)
            VALUES (:accountId, :nickname, :avatarUrl, :bio, :createdAt, :updatedAt)
            """;

    private final JdbcClient jdbcClient;

    public JdbcUserProfileDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UserProfileRecord> findByAccountId(long accountId) {
        try {
            return jdbcClient.sql(FIND_BY_ACCOUNT_ID_SQL)
                    .param("accountId", accountId)
                    .query((rs, rowNum) -> new UserProfileRecord(
                            rs.getLong("account_id"),
                            rs.getString("nickname"),
                            rs.getString("avatar_url"),
                            rs.getString("bio"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant()
                    ))
                    .optional();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query user profile by account id", exception);
        }
    }

    @Override
    public void insert(UserProfileRecord record) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("accountId", record.accountId())
                    .param("nickname", record.nickname())
                    .param("avatarUrl", record.avatarUrl())
                    .param("bio", record.bio())
                    .param("createdAt", Timestamp.from(record.createdAt()))
                    .param("updatedAt", Timestamp.from(record.updatedAt()))
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert user profile", exception);
        }
    }

    @Override
    public void update(UserProfileRecord record) {
        try {
            int updatedRows = jdbcClient.sql(UPDATE_SQL)
                    .param("accountId", record.accountId())
                    .param("nickname", record.nickname())
                    .param("avatarUrl", record.avatarUrl())
                    .param("bio", record.bio())
                    .param("updatedAt", Timestamp.from(record.updatedAt()))
                    .update();

            if (updatedRows == 0) {
                throw new DatabaseServiceException("user profile update affected no rows");
            }
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to update user profile", exception);
        }
    }
}

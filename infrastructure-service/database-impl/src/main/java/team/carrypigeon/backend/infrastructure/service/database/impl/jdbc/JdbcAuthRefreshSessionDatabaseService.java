package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthRefreshSessionRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthRefreshSessionDatabaseService;

/**
 * JDBC 刷新会话数据库服务。
 * 职责：在 database-impl 中完成 refresh session 的最小查询、写入与撤销。
 * 边界：只负责数据库记录映射，不承载 token 业务规则。
 */
public class JdbcAuthRefreshSessionDatabaseService implements AuthRefreshSessionDatabaseService {

    private static final String FIND_BY_ID_SQL = """
            SELECT id, account_id, refresh_token_hash, expires_at, revoked, created_at, updated_at
            FROM auth_refresh_session
            WHERE id = :id
            """;

    private static final String INSERT_SQL = """
            INSERT INTO auth_refresh_session (id, account_id, refresh_token_hash, expires_at, revoked, created_at, updated_at)
            VALUES (:id, :accountId, :refreshTokenHash, :expiresAt, :revoked, :createdAt, :updatedAt)
            """;

    private static final String REVOKE_SQL = """
            UPDATE auth_refresh_session
            SET revoked = true, updated_at = CURRENT_TIMESTAMP(6)
            WHERE id = :id
            """;

    private final JdbcClient jdbcClient;

    public JdbcAuthRefreshSessionDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<AuthRefreshSessionRecord> findById(long sessionId) {
        try {
            return jdbcClient.sql(FIND_BY_ID_SQL)
                    .param("id", sessionId)
                    .query((rs, rowNum) -> new AuthRefreshSessionRecord(
                            rs.getLong("id"),
                            rs.getLong("account_id"),
                            rs.getString("refresh_token_hash"),
                            rs.getTimestamp("expires_at").toInstant(),
                            rs.getBoolean("revoked"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant()
                    ))
                    .optional();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query auth refresh session by id", exception);
        }
    }

    @Override
    public void insert(AuthRefreshSessionRecord record) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("id", record.id())
                    .param("accountId", record.accountId())
                    .param("refreshTokenHash", record.refreshTokenHash())
                    .param("expiresAt", Timestamp.from(record.expiresAt()))
                    .param("revoked", record.revoked())
                    .param("createdAt", Timestamp.from(record.createdAt()))
                    .param("updatedAt", Timestamp.from(record.updatedAt()))
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert auth refresh session", exception);
        }
    }

    @Override
    public void revoke(long sessionId) {
        try {
            jdbcClient.sql(REVOKE_SQL)
                    .param("id", sessionId)
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to revoke auth refresh session", exception);
        }
    }
}

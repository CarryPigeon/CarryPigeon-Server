package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.exception.DatabaseServiceException;
import team.carrypigeon.backend.infrastructure.service.database.api.model.AuthAccountRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.AuthAccountDatabaseService;

/**
 * JDBC 鉴权账户数据库服务。
 * 职责：在 database-impl 中完成鉴权账户的最小查询与写入。
 * 边界：只负责数据库记录映射，不承载注册业务规则与协议决策。
 */
public class JdbcAuthAccountDatabaseService implements AuthAccountDatabaseService {

    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, username, password_hash, created_at, updated_at
            FROM auth_account
            WHERE username = :username
            """;

    private static final String INSERT_SQL = """
            INSERT INTO auth_account (id, username, password_hash, created_at, updated_at)
            VALUES (:id, :username, :passwordHash, :createdAt, :updatedAt)
            """;

    private final JdbcClient jdbcClient;

    public JdbcAuthAccountDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<AuthAccountRecord> findByUsername(String username) {
        try {
            return jdbcClient.sql(FIND_BY_USERNAME_SQL)
                    .param("username", username)
                    .query((rs, rowNum) -> new AuthAccountRecord(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant()
                    ))
                    .optional();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to query auth account by username", exception);
        }
    }

    @Override
    public void insert(AuthAccountRecord record) {
        try {
            jdbcClient.sql(INSERT_SQL)
                    .param("id", record.id())
                    .param("username", record.username())
                    .param("passwordHash", record.passwordHash())
                    .param("createdAt", Timestamp.from(record.createdAt()))
                    .param("updatedAt", Timestamp.from(record.updatedAt()))
                    .update();
        } catch (DataAccessException exception) {
            throw new DatabaseServiceException("failed to insert auth account", exception);
        }
    }
}

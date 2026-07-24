package team.carrypigeon.backend.infrastructure.service.database.impl.jdbc;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import team.carrypigeon.backend.infrastructure.service.database.api.model.PluginMigrationRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.PluginMigrationDatabaseService;

/**
 * JDBC 插件迁移历史服务。
 * 职责：使用宿主 DataSource 维护 `plugin_schema_history` 表。
 * 边界：只记录迁移元数据，不执行插件自身迁移内容。
 */
public class JdbcPluginMigrationDatabaseService implements PluginMigrationDatabaseService {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS plugin_schema_history (
                plugin_id VARCHAR(128) NOT NULL,
                plugin_version VARCHAR(64) NOT NULL,
                migration_version VARCHAR(64) NOT NULL,
                description VARCHAR(255) NOT NULL,
                checksum CHAR(64) NOT NULL,
                executed_at DATETIME(6) NOT NULL,
                success BOOLEAN NOT NULL,
                PRIMARY KEY (plugin_id, migration_version)
            )
            """;

    private final JdbcClient jdbcClient;

    public JdbcPluginMigrationDatabaseService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void ensureHistoryStorage() {
        jdbcClient.sql(CREATE_TABLE_SQL).update();
    }

    @Override
    public Optional<PluginMigrationRecord> find(String pluginId, String migrationVersion) {
        return jdbcClient.sql("""
                        SELECT plugin_id, plugin_version, migration_version, description,
                               checksum, executed_at, success
                        FROM plugin_schema_history
                        WHERE plugin_id = :pluginId AND migration_version = :migrationVersion
                        """)
                .param("pluginId", pluginId)
                .param("migrationVersion", migrationVersion)
                .query((resultSet, rowNumber) -> new PluginMigrationRecord(
                        resultSet.getString("plugin_id"),
                        resultSet.getString("plugin_version"),
                        resultSet.getString("migration_version"),
                        resultSet.getString("description"),
                        resultSet.getString("checksum"),
                        resultSet.getTimestamp("executed_at").toInstant(),
                        resultSet.getBoolean("success")
                ))
                .optional();
    }

    @Override
    public void insert(PluginMigrationRecord record) {
        jdbcClient.sql("""
                        INSERT INTO plugin_schema_history (
                            plugin_id, plugin_version, migration_version, description,
                            checksum, executed_at, success
                        ) VALUES (
                            :pluginId, :pluginVersion, :migrationVersion, :description,
                            :checksum, :executedAt, :success
                        )
                        """)
                .param("pluginId", record.pluginId())
                .param("pluginVersion", record.pluginVersion())
                .param("migrationVersion", record.migrationVersion())
                .param("description", record.description())
                .param("checksum", record.checksum())
                .param("executedAt", Timestamp.from(record.executedAt()))
                .param("success", record.success())
                .update();
    }
}

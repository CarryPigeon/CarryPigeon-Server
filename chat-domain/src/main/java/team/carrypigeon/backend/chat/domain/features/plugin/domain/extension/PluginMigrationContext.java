package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifest;

/**
 * 插件迁移上下文。
 * 职责：向已授权 SYSTEM 插件迁移提供宿主数据源、Spring Context 和 Manifest。
 * 边界：不创建新的连接池，不改变核心事务边界。
 */
public record PluginMigrationContext(
        DataSource dataSource,
        ApplicationContext applicationContext,
        PluginManifest manifest
) {

    public PluginMigrationContext {
        dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        applicationContext = Objects.requireNonNull(applicationContext, "applicationContext must not be null");
        manifest = Objects.requireNonNull(manifest, "manifest must not be null");
    }
}

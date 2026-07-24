package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

/**
 * 插件数据库迁移契约。
 * 职责：描述一个插件版本化、可校验的启动期迁移。
 * 边界：迁移执行由插件运行时协调，历史记录由 database-api 实现；插件可以通过上下文直接使用宿主 DataSource。
 */
public interface PluginMigration {

    String pluginId();

    String pluginVersion();

    String migrationVersion();

    String description();

    String checksum();

    void migrate(PluginMigrationContext context);
}

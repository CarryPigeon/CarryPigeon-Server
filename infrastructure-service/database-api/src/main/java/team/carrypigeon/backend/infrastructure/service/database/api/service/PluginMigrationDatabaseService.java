package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.PluginMigrationRecord;

/**
 * 插件迁移历史数据库服务。
 * 职责：为插件运行时提供迁移历史表初始化、查询和成功记录能力。
 * 边界：不执行插件提供的迁移 SQL，也不解释插件业务数据。
 */
public interface PluginMigrationDatabaseService {

    /**
     * 确保插件迁移历史存储存在。
     */
    void ensureHistoryStorage();

    /**
     * 查询指定插件迁移版本的执行记录。
     *
     * @param pluginId 插件唯一标识
     * @param migrationVersion 迁移版本
     * @return 已执行时返回历史记录
     */
    Optional<PluginMigrationRecord> find(String pluginId, String migrationVersion);

    /**
     * 保存成功执行的插件迁移记录。
     *
     * @param record 迁移历史记录
     */
    void insert(PluginMigrationRecord record);
}

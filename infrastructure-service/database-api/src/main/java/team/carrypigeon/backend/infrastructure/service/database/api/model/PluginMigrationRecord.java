package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 插件数据库迁移历史记录。
 * 职责：在 database-api 边界表达已执行插件迁移的最小审计信息。
 *
 * @param pluginId 插件唯一标识
 * @param pluginVersion 执行迁移时的插件版本
 * @param migrationVersion 插件内迁移版本
 * @param description 迁移说明
 * @param checksum 迁移内容摘要
 * @param executedAt 执行完成时间
 * @param success 是否成功
 */
public record PluginMigrationRecord(
        String pluginId,
        String pluginVersion,
        String migrationVersion,
        String description,
        String checksum,
        Instant executedAt,
        boolean success
) {
}

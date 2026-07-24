package team.carrypigeon.backend.chat.domain.features.plugin.domain.projection;

/**
 * 插件运行时状态投影。
 *
 * @param pluginId 插件 ID
 * @param version 插件版本
 * @param status 当前状态
 * @param message 状态说明
 */
public record PluginRuntimeStatusResult(
        String pluginId,
        String version,
        String status,
        String message
) {
}

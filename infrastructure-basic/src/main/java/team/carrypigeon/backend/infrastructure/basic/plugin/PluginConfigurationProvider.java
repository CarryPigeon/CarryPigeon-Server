package team.carrypigeon.backend.infrastructure.basic.plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 插件配置查询入口。
 * 职责：为上层模块和后续插件提供统一、只读的插件配置查询能力。
 * 边界：这里只查询配置，不负责插件实例管理、生命周期调度或业务开关解释。
 */
public class PluginConfigurationProvider {

    private final PluginProperties properties;

    public PluginConfigurationProvider(PluginProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * 获取全部插件配置快照。
     *
     * @return 按插件 ID 索引的只读配置
     */
    public Map<String, PluginProperties.PluginConfig> configs() {
        return properties.configs();
    }

    /**
     * 查询指定插件配置。
     *
     * @param pluginId 插件唯一标识
     * @return 已配置插件返回配置，否则返回空
     */
    public Optional<PluginProperties.PluginConfig> find(String pluginId) {
        return Optional.ofNullable(properties.configs().get(requirePluginId(pluginId)));
    }

    /**
     * 判断插件是否显式启用。
     *
     * @param pluginId 插件唯一标识
     * @return 仅当插件存在且 enabled=true 时返回 true
     */
    public boolean isEnabled(String pluginId) {
        return find(pluginId)
                .map(PluginProperties.PluginConfig::enabled)
                .orElse(false);
    }

    private static String requirePluginId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        return pluginId;
    }
}

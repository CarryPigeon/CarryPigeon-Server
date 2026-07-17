package team.carrypigeon.backend.infrastructure.basic.plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 全局插件配置属性。
 * 职责：绑定 `cp.plugin.configs` 下的插件启停状态与插件自定义选项。
 * 边界：这里只承载插件配置结构，不负责插件加载、执行、权限或业务语义解释。
 */
@ConfigurationProperties(prefix = "cp.plugin")
public record PluginProperties(Map<String, PluginConfig> configs) {

    @ConstructorBinding
    public PluginProperties {
        configs = copyConfigs(configs);
    }

    public PluginProperties() {
        this(Map.of());
    }

    private static Map<String, PluginConfig> copyConfigs(Map<String, PluginConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Map.of();
        }
        Map<String, PluginConfig> copied = new LinkedHashMap<>();
        configs.forEach((pluginId, config) -> {
            validatePluginId(pluginId);
            copied.put(pluginId, config == null ? new PluginConfig() : config);
        });
        return Map.copyOf(copied);
    }

    private static void validatePluginId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("cp.plugin.configs key must not be blank");
        }
    }

    /**
     * 单个插件的配置结构。
     * 职责：表达插件级启停状态与由插件自身解释的自定义选项。
     * 边界：options 只保存结构化配置值，不在基建层解释具体含义。
     */
    public record PluginConfig(boolean enabled, Map<String, Object> options) {

        @ConstructorBinding
        public PluginConfig {
            options = copyOptions(options);
        }

        public PluginConfig() {
            this(false, Map.of());
        }

        private static Map<String, Object> copyOptions(Map<String, Object> options) {
            if (options == null || options.isEmpty()) {
                return Map.of();
            }
            Map<String, Object> copied = new LinkedHashMap<>();
            options.forEach((optionName, value) -> {
                if (optionName == null || optionName.isBlank()) {
                    throw new IllegalArgumentException("cp.plugin.configs.*.options key must not be blank");
                }
                copied.put(optionName, value);
            });
            return Map.copyOf(copied);
        }
    }
}

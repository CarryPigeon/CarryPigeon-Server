package team.carrypigeon.backend.infrastructure.basic.plugin;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证插件配置属性模型。
 * 职责：确保绑定后的插件配置具备默认值、基础校验和只读边界。
 * 边界：不验证 Spring Boot 绑定过程，绑定契约由自动配置测试覆盖。
 */
@Tag("unit")
class PluginPropertiesTests {

    /**
     * 测试默认配置。
     * 输入：无显式插件配置。
     * 期望：configs 为空，表达没有任何插件被启用。
     */
    @Test
    void configs_createdWithDefaultConstructor_returnEmptyConfigs() {
        PluginProperties properties = new PluginProperties();

        assertTrue(properties.configs().isEmpty());
    }

    /**
     * 测试插件配置规范化。
     * 输入：启用插件和自定义 options。
     * 期望：启停状态与 options 保留，且对外 Map 不可变。
     */
    @Test
    void configs_createdWithPluginConfig_returnReadOnlySnapshot() {
        PluginProperties.PluginConfig pluginConfig = new PluginProperties.PluginConfig(
                true,
                Map.of("provider", "local")
        );

        PluginProperties properties = new PluginProperties(Map.of("translator", pluginConfig));

        assertTrue(properties.configs().get("translator").enabled());
        assertEquals("local", properties.configs().get("translator").options().get("provider"));
        assertThrows(UnsupportedOperationException.class, () -> properties.configs().put("other", new PluginProperties.PluginConfig()));
        assertThrows(UnsupportedOperationException.class, () -> pluginConfig.options().put("provider", "remote"));
    }

    /**
     * 测试空插件配置。
     * 输入：插件配置对象为空。
     * 期望：归一化为默认禁用且 options 为空，避免调用方处理 null。
     */
    @Test
    void configs_createdWithNullPluginConfig_useDisabledDefaults() {
        Map<String, PluginProperties.PluginConfig> configs = new HashMap<>();
        configs.put("translator", null);

        PluginProperties properties = new PluginProperties(configs);

        PluginProperties.PluginConfig pluginConfig = properties.configs().get("translator");

        assertFalse(pluginConfig.enabled());
        assertTrue(pluginConfig.options().isEmpty());
    }

    /**
     * 测试非法插件 ID。
     * 输入：空白插件 ID。
     * 期望：绑定阶段快速失败，避免产生不可检索配置。
     */
    @Test
    void configs_createdWithBlankPluginId_throwIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginProperties(Map.of(" ", new PluginProperties.PluginConfig()))
        );
    }

    /**
     * 测试非法 option 名称。
     * 输入：空白 option 名称。
     * 期望：绑定阶段快速失败，避免插件读取到无语义配置项。
     */
    @Test
    void options_createdWithBlankOptionName_throwIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginProperties.PluginConfig(true, Map.of(" ", "value"))
        );
    }
}

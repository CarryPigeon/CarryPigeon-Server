package team.carrypigeon.backend.infrastructure.basic.plugin;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证插件配置查询入口。
 * 职责：确保上层模块通过统一入口查询插件配置时获得稳定默认行为。
 * 边界：不解释 options 内具体业务含义。
 */
@Tag("unit")
class PluginConfigurationProviderTests {

    /**
     * 测试已配置插件查询。
     * 输入：一个已启用插件配置。
     * 期望：find 返回配置，isEnabled 返回 true。
     */
    @Test
    void isEnabled_configuredEnabledPlugin_returnTrue() {
        PluginConfigurationProvider provider = new PluginConfigurationProvider(new PluginProperties(Map.of(
                "translator",
                new PluginProperties.PluginConfig(true, Map.of())
        )));

        assertTrue(provider.find("translator").isPresent());
        assertTrue(provider.isEnabled("translator"));
    }

    /**
     * 测试未配置插件查询。
     * 输入：不存在的插件 ID。
     * 期望：find 返回空，isEnabled 默认 false。
     */
    @Test
    void isEnabled_missingPlugin_returnFalse() {
        PluginConfigurationProvider provider = new PluginConfigurationProvider(new PluginProperties());

        assertTrue(provider.find("translator").isEmpty());
        assertFalse(provider.isEnabled("translator"));
    }

    /**
     * 测试非法查询参数。
     * 输入：空白插件 ID。
     * 期望：查询入口快速失败，避免调用方误以为插件未配置。
     */
    @Test
    void find_blankPluginId_throwIllegalArgumentException() {
        PluginConfigurationProvider provider = new PluginConfigurationProvider(new PluginProperties());

        assertThrows(IllegalArgumentException.class, () -> provider.find(" "));
        assertThrows(IllegalArgumentException.class, () -> provider.isEnabled(null));
    }
}

package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginManifestCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 分发插件预检命令测试。
 * 职责：验证命令复用正式 Manifest 预检且不会接受不受支持的运行参数。
 */
class PluginPreflightCommandTests {

    /**
     * 验证无外部插件的测试 classpath 可以在不启动 Spring 的情况下完成预检。
     */
    @Test
    void verify_noExternalPlugin_returnsEmptyCatalog() {
        PluginManifestCatalog catalog = PluginPreflightCommand.verify(getClass().getClassLoader());

        assertEquals(0, catalog.manifests().size());
    }

    /**
     * 验证命令拒绝未知参数，避免验证入口产生未定义模式。
     */
    @Test
    void main_argumentProvided_rejectsInvocation() {
        assertThrows(IllegalArgumentException.class, () -> PluginPreflightCommand.main(new String[]{"--unknown"}));
    }
}

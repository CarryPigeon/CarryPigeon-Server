package team.carrypigeon.backend.starter.config;

import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.plugin.manifest.PluginHostIdentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 宿主构建身份资源测试。
 * 职责：验证 Maven 过滤后的宿主版本和构建指纹可供插件预检直接使用。
 */
class PluginHostIdentityResourceTests {

    /**
     * 验证测试运行时读取到的是实际构建值而不是未展开占位符。
     */
    @Test
    void load_filteredResource_returnsConcreteBuildIdentity() {
        PluginHostIdentity identity = PluginHostIdentity.load(getClass().getClassLoader());

        assertEquals("1.0.0", identity.version());
        assertEquals("development", identity.buildHash());
    }
}

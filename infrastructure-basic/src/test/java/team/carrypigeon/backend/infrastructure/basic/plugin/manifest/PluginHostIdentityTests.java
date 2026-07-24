package team.carrypigeon.backend.infrastructure.basic.plugin.manifest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 插件宿主身份测试。
 * 职责：验证 Manifest 兼容性使用 Spring Boot 版本，而不是 Spring Framework 版本。
 */
class PluginHostIdentityTests {

    /**
     * 验证运行时宿主身份与 Spring Boot 构件版本一致。
     */
    @Test
    void load_runtimeClasspath_usesSpringBootVersion() {
        PluginHostIdentity identity = PluginHostIdentity.load(getClass().getClassLoader());

        assertEquals(SpringBootVersion.getVersion(), identity.springBootVersion());
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证数据库实现配置的最小契约。
 * 职责：确保 database-impl 的默认开关稳定可用。
 * 边界：不验证 Spring 绑定流程，只验证配置对象自身默认值。
 */
@Tag("unit")
class DatabaseServicePropertiesTests {

    /**
     * 测试默认数据库配置。
     * 输入：不传入任何自定义配置。
     * 期望：默认启用数据库实现。
     */
    @Test
    void constructor_default_usesStableDefaults() {
        DatabaseServiceProperties properties = new DatabaseServiceProperties();

        assertEquals(true, properties.enabled());
    }
}

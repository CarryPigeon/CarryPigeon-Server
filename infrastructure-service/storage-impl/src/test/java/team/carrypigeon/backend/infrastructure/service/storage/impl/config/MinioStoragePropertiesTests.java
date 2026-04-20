package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 MinIO 存储配置的最小契约。
 * 职责：确保 storage-impl 的默认连接信息和 bucket 稳定可用。
 * 边界：不验证 Spring 绑定流程，只验证配置对象自身默认值。
 */
class MinioStoragePropertiesTests {

    /**
     * 测试默认对象存储配置。
     * 输入：不传入任何自定义配置。
     * 期望：启用对象存储实现，并使用本地开发默认 MinIO 配置。
     */
    @Test
    void constructor_default_usesStableDefaults() {
        MinioStorageProperties properties = new MinioStorageProperties();

        assertEquals(true, properties.enabled());
        assertEquals("http://127.0.0.1:9000", properties.endpoint());
        assertEquals("carrypigeon", properties.bucket());
    }
}

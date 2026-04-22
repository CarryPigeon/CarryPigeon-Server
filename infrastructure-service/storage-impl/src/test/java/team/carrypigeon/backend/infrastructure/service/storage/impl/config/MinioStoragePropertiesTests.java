package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 MinIO 存储配置的最小契约。
 * 职责：确保 storage-impl 只为非敏感连接信息保留最小默认值，不为访问凭据提供固定回退。
 * 边界：不验证 Spring 绑定流程，只验证配置对象自身默认值。
 */
class MinioStoragePropertiesTests {

    /**
     * 测试默认对象存储配置。
     * 输入：不传入任何自定义配置。
     * 期望：默认关闭对象存储实现，并保留 endpoint 与 bucket 默认值，同时不提供敏感凭据默认值。
     */
    @Test
    void constructor_default_usesStableDefaults() {
        MinioStorageProperties properties = new MinioStorageProperties(false, null, null, null, null);

        assertEquals(false, properties.enabled());
        assertEquals("http://127.0.0.1:9000", properties.endpoint());
        assertEquals("carrypigeon", properties.bucket());
        assertEquals("", properties.accessKey());
        assertEquals("", properties.secretKey());
    }

    /**
     * 测试启用对象存储但缺失 access key 时会在配置对象创建阶段失败。
     */
    @Test
    void constructor_enabledWithoutAccessKey_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinioStorageProperties(true, "http://127.0.0.1:9000", "", "test-secret", "carrypigeon")
        );
    }

    /**
     * 测试启用对象存储但缺失 secret key 时会在配置对象创建阶段失败。
     */
    @Test
    void constructor_enabledWithoutSecretKey_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinioStorageProperties(true, "http://127.0.0.1:9000", "test-access", "", "carrypigeon")
        );
    }
}

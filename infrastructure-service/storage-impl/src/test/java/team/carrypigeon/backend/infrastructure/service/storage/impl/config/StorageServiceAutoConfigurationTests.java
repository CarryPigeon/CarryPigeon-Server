package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheck;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证对象存储自动配置的装配边界。
 * 职责：确保 storage-impl 只在启用条件满足时注册对象存储相关 Bean。
 * 边界：不访问真实 MinIO，只验证自动配置的上下文装配行为。
 */
@Tag("contract")
class StorageServiceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    StorageServiceAutoConfiguration.class
            ));

    /**
     * 测试启用对象存储服务时的自动配置。
     * 输入：启用开关和 MinIO 最小连接配置。
     * 期望：成功装配 MinIO 客户端、对象存储服务和健康检查服务。
     */
    @Test
    void autoConfiguration_enabled_registersStorageBeans() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.storage.enabled=true",
                        "cp.infrastructure.service.storage.endpoint=http://127.0.0.1:9000",
                        "cp.infrastructure.service.storage.access-key=test-access-key",
                        "cp.infrastructure.service.storage.secret-key=test-secret-key",
                        "cp.infrastructure.service.storage.bucket=carrypigeon"
                )
                .withBean(MinioClient.class, () -> mock(MinioClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(MinioClient.class);
                    assertThat(context).hasSingleBean(ObjectStorageService.class);
                    assertThat(context).hasSingleBean(StorageHealthService.class);
                    assertThat(context).hasSingleBean(InitializationCheck.class);
                });
    }

    /**
     * 测试禁用对象存储服务时的自动配置。
     * 输入：禁用开关和 MinIO 最小连接配置。
     * 期望：storage-impl 不注册对象存储相关 Bean。
     */
    @Test
    void autoConfiguration_disabled_skipsStorageBeans() {
        contextRunner
                .withPropertyValues(
                        "cp.infrastructure.service.storage.enabled=false",
                        "cp.infrastructure.service.storage.endpoint=http://127.0.0.1:9000",
                        "cp.infrastructure.service.storage.access-key=test-access-key",
                        "cp.infrastructure.service.storage.secret-key=test-secret-key",
                        "cp.infrastructure.service.storage.bucket=carrypigeon"
                )
                .withBean(MinioClient.class, () -> mock(MinioClient.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ObjectStorageService.class);
                    assertThat(context).doesNotHaveBean(StorageHealthService.class);
                    assertThat(context).doesNotHaveBean(InitializationCheck.class);
                });
    }
}

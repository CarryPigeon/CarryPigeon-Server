package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;
import team.carrypigeon.backend.infrastructure.service.storage.impl.minio.MinioObjectStorageService;
import team.carrypigeon.backend.infrastructure.service.storage.impl.minio.MinioStorageHealthService;

/**
 * 对象存储自动配置。
 * 职责：在 storage-impl 内装配 MinIO 对象存储实现。
 * 边界：只创建具体实现 Bean，不在 API 模块中定义 Spring 装配逻辑。
 */
@AutoConfiguration
@EnableConfigurationProperties(MinioStorageProperties.class)
@ConditionalOnProperty(prefix = "cp.infrastructure.service.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageServiceAutoConfiguration {

    /**
     * 创建 MinIO 客户端。
     *
     * @param properties MinIO 存储配置
     * @return MinIO 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    /**
     * 创建对象存储服务。
     *
     * @param minioClient MinIO 客户端
     * @param properties MinIO 存储配置
     * @return 对象存储服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectStorageService objectStorageService(MinioClient minioClient, MinioStorageProperties properties) {
        return new MinioObjectStorageService(minioClient, properties);
    }

    /**
     * 创建对象存储健康检查服务。
     *
     * @param minioClient MinIO 客户端
     * @param properties MinIO 存储配置
     * @return 对象存储健康检查服务
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageHealthService storageHealthService(MinioClient minioClient, MinioStorageProperties properties) {
        return new MinioStorageHealthService(minioClient, properties);
    }
}

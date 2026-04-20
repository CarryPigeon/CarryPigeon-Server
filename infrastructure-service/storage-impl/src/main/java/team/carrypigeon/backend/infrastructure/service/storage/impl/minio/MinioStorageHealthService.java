package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;

/**
 * MinIO 对象存储健康检查实现。
 * 职责：通过 bucket 存在性校验验证对象存储服务可用性。
 * 边界：MinIO SDK 细节保持在 impl 内部。
 */
public class MinioStorageHealthService implements StorageHealthService {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public MinioStorageHealthService(MinioClient minioClient, MinioStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StorageHealth check() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.bucket())
                            .build()
            );
            return new StorageHealth(exists, "storage bucket check completed");
        } catch (Exception ex) {
            return new StorageHealth(false, "storage bucket check failed: " + ex.getClass().getSimpleName());
        }
    }
}

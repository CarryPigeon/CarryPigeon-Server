package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置。
 * 职责：承载 storage-impl 所需的 MinIO 连接信息与默认 bucket。
 * 边界：配置类属于具体对象存储实现模块，不进入基础设施固定层或业务层。
 *
 * @param enabled 是否启用对象存储实现
 * @param endpoint MinIO 服务地址
 * @param accessKey MinIO 访问 key
 * @param secretKey MinIO 密钥
 * @param bucket 默认 bucket
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.storage")
public record MinioStorageProperties(
        boolean enabled,
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {

    public MinioStorageProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://127.0.0.1:9000";
        }
        if (accessKey == null) {
            accessKey = "";
        }
        if (secretKey == null) {
            secretKey = "";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "carrypigeon";
        }
        if (enabled && accessKey.isBlank()) {
            throw new IllegalArgumentException("cp.infrastructure.service.storage.access-key must not be blank when storage is enabled");
        }
        if (enabled && secretKey.isBlank()) {
            throw new IllegalArgumentException("cp.infrastructure.service.storage.secret-key must not be blank when storage is enabled");
        }
    }

}

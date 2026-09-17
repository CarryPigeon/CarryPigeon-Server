package team.carrypigeon.backend.infrastructure.service.storage.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

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
 * @param connectTimeout 建连超时
 * @param readTimeout 读取停顿超时
 * @param writeTimeout 写入停顿超时
 */
@ConfigurationProperties(prefix = "cp.infrastructure.service.storage")
public record MinioStorageProperties(
        boolean enabled,
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout
) {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(30);

    @ConstructorBinding
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
        connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT, "connect-timeout");
        readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT, "read-timeout");
        writeTimeout = positiveOrDefault(writeTimeout, DEFAULT_WRITE_TIMEOUT, "write-timeout");
        if (enabled && accessKey.isBlank()) {
            throw new IllegalArgumentException("cp.infrastructure.service.storage.access-key must not be blank when storage is enabled");
        }
        if (enabled && secretKey.isBlank()) {
            throw new IllegalArgumentException("cp.infrastructure.service.storage.secret-key must not be blank when storage is enabled");
        }
    }

    public MinioStorageProperties(
            boolean enabled,
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {
        this(enabled, endpoint, accessKey, secretKey, bucket, null, null, null);
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue, String propertyName) {
        Duration resolved = value == null ? defaultValue : value;
        if (resolved.isZero() || resolved.isNegative()) {
            throw new IllegalArgumentException("cp.infrastructure.service.storage." + propertyName + " must be positive");
        }
        return resolved;
    }

}

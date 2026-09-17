package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import team.carrypigeon.backend.infrastructure.basic.logging.LogValueSanitizer;
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

    /**
     * 执行对象存储健康检查。
     * 输出：返回 bucket 可访问性以及诊断消息。
     *
     * @return 对象存储健康检查结果
     */
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
            return new StorageHealth(false, "storage bucket check failed: " + diagnosticMessage(ex));
        }
    }

    private String diagnosticMessage(Exception ex) {
        if (ex instanceof ErrorResponseException errorResponseException) {
            return minioErrorDiagnostic(errorResponseException);
        }
        return LogValueSanitizer.singleLine(ex.getClass().getSimpleName()
                + ": " + valueOrFallback(ex.getMessage(), "no message")
                + ", endpoint=" + properties.endpoint()
                + ", bucket=" + properties.bucket());
    }

    private String minioErrorDiagnostic(ErrorResponseException ex) {
        ErrorResponse errorResponse = ex.errorResponse();
        String code = errorResponse == null ? "unknown" : valueOrFallback(errorResponse.code(), "unknown");
        String message = errorResponse == null ? "no message" : valueOrFallback(errorResponse.message(), "no message");
        String requestId = errorResponse == null ? "unknown" : valueOrFallback(errorResponse.requestId(), "unknown");
        String resource = errorResponse == null ? "unknown" : valueOrFallback(errorResponse.resource(), "unknown");
        int httpStatus = ex.response() == null ? 0 : ex.response().code();
        return LogValueSanitizer.singleLine("MinIO "
                + "code=" + code
                + ", message=" + message
                + ", httpStatus=" + httpStatus
                + ", endpoint=" + properties.endpoint()
                + ", bucket=" + properties.bucket()
                + ", resource=" + resource
                + ", requestId=" + requestId);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

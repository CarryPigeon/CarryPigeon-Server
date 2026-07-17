package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinioStorageHealthService 契约测试。
 * 职责：验证 MinIO 健康检查对 bucket 存在性和异常场景的稳定映射。
 * 边界：不访问真实 MinIO，只验证健康检查语义。
 */
@Tag("contract")
class MinioStorageHealthServiceTests {

    private static final MinioStorageProperties PROPERTIES = new MinioStorageProperties(
            true,
            "http://127.0.0.1:9000",
            "test-access",
            "test-secret",
            "carrypigeon"
    );

    /**
     * 验证 bucket 存在时会返回 available=true 的健康结果。
     */
    @Test
    @DisplayName("check existing bucket returns available health")
    void check_existingBucket_returnsAvailableHealth() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        MinioStorageHealthService service = new MinioStorageHealthService(minioClient, PROPERTIES);

        StorageHealth result = service.check();

        ArgumentCaptor<BucketExistsArgs> argsCaptor = ArgumentCaptor.forClass(BucketExistsArgs.class);
        verify(minioClient).bucketExists(argsCaptor.capture());
        assertEquals("carrypigeon", argsCaptor.getValue().bucket());
        assertTrue(result.available());
        assertEquals("storage bucket check completed", result.message());
    }

    /**
     * 验证 bucket 不存在时会返回 available=false 但保持稳定说明。
     */
    @Test
    @DisplayName("check missing bucket returns unavailable health")
    void check_missingBucket_returnsUnavailableHealth() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MinioStorageHealthService service = new MinioStorageHealthService(minioClient, PROPERTIES);

        StorageHealth result = service.check();

        assertFalse(result.available());
        assertEquals("storage bucket check completed", result.message());
    }

    /**
     * 验证 MinIO 客户端异常时会返回 unavailable 健康结果而不是抛出异常。
     */
    @Test
    @DisplayName("check client failure returns unavailable health")
    void check_clientFailure_returnsUnavailableHealth() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new IllegalStateException("bucket check failed"));
        MinioStorageHealthService service = new MinioStorageHealthService(minioClient, PROPERTIES);

        StorageHealth result = service.check();

        assertFalse(result.available());
        assertEquals(
                "storage bucket check failed: IllegalStateException: bucket check failed, endpoint=http://127.0.0.1:9000, bucket=carrypigeon",
                result.message()
        );
    }

    /**
     * 验证 MinIO 错误响应会保留错误码、请求标识和连接目标，便于启动失败日志直接定位配置问题。
     */
    @Test
    @DisplayName("check minio error response returns actionable diagnostic")
    void check_minioErrorResponse_returnsActionableDiagnostic() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(minioError(
                "InvalidAccessKeyId",
                "The Access Key Id you provided does not exist in our records.",
                403,
                "request-123"
        ));
        MinioStorageHealthService service = new MinioStorageHealthService(minioClient, PROPERTIES);

        StorageHealth result = service.check();

        assertFalse(result.available());
        assertAll(
                () -> assertTrue(result.message().contains("storage bucket check failed: MinIO code=InvalidAccessKeyId")),
                () -> assertTrue(result.message().contains("message=The Access Key Id you provided does not exist in our records.")),
                () -> assertTrue(result.message().contains("httpStatus=403")),
                () -> assertTrue(result.message().contains("endpoint=http://127.0.0.1:9000")),
                () -> assertTrue(result.message().contains("bucket=carrypigeon")),
                () -> assertTrue(result.message().contains("resource=/carrypigeon")),
                () -> assertTrue(result.message().contains("requestId=request-123"))
        );
    }

    private static ErrorResponseException minioError(String code, String message, int status, String requestId) {
        ErrorResponse errorResponse = new ErrorResponse(
                code,
                message,
                "carrypigeon",
                "",
                "/carrypigeon",
                requestId,
                "host-123"
        );
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://127.0.0.1:9000/carrypigeon").build())
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("Forbidden")
                .build();
        return new ErrorResponseException(errorResponse, response, "minio error");
    }
}

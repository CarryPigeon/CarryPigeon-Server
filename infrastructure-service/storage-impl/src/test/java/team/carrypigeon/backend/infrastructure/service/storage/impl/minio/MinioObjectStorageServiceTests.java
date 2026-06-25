package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team.carrypigeon.backend.infrastructure.service.storage.api.exception.StorageServiceException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinioObjectStorageService 契约测试。
 * 职责：验证 MinIO 对象存储实现对 storage-api 稳定契约的映射与异常语义。
 * 边界：不访问真实 MinIO，只验证 SDK 调用参数与返回/失败行为。
 */
@Tag("contract")
class MinioObjectStorageServiceTests {

    private static final MinioStorageProperties PROPERTIES = new MinioStorageProperties(
            true,
            "http://127.0.0.1:9000",
            "test-access",
            "test-secret",
            "carrypigeon"
    );

    /**
     * 验证上传对象时会把 bucket、objectKey、contentType 和 size 下发给 MinIO 客户端。
     */
    @Test
    @DisplayName("put valid command delegates bucket key and content metadata")
    void put_validCommand_delegatesBucketKeyAndContentMetadata() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient, PROPERTIES);
        byte[] content = "hello minio".getBytes();

        StorageObject result = service.put(new PutObjectCommand(
                "attachments/hello.txt",
                new ByteArrayInputStream(content),
                content.length,
                "text/plain"
        ));

        ArgumentCaptor<PutObjectArgs> argsCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(argsCaptor.capture());
        PutObjectArgs args = argsCaptor.getValue();
        assertEquals("carrypigeon", args.bucket());
        assertEquals("attachments/hello.txt", args.object());
        assertEquals("text/plain", args.contentType());
        assertEquals("attachments/hello.txt", result.objectKey());
        assertEquals("text/plain", result.contentType());
        assertEquals(content.length, result.size());
        assertFalse(result.content().isPresent());
    }

    /**
     * 验证读取存在对象时只返回元信息，不会额外打开对象内容流。
     */
    @Test
    @DisplayName("get existing object returns metadata only")
    void get_existingObject_returnsMetadataOnly() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        StatObjectResponse statObjectResponse = mock(StatObjectResponse.class);
        when(statObjectResponse.contentType()).thenReturn("image/png");
        when(statObjectResponse.size()).thenReturn(1024L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient, PROPERTIES);

        Optional<StorageObject> result = service.get(new GetObjectCommand("attachments/image.png"));

        assertTrue(result.isPresent());
        StorageObject storageObject = result.orElseThrow();
        assertEquals("attachments/image.png", storageObject.objectKey());
        assertEquals("image/png", storageObject.contentType());
        assertEquals(1024L, storageObject.size());
        assertFalse(storageObject.content().isPresent());
        verify(minioClient, never()).getObject(any());
    }

    /**
     * 验证读取不存在对象时会返回 Optional.empty 而不是抛出异常。
     */
    @Test
    @DisplayName("get missing object returns empty optional")
    void get_missingObject_returnsEmptyOptional() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ErrorResponseException cause = mock(ErrorResponseException.class);
        io.minio.messages.ErrorResponse errorResponse = mock(io.minio.messages.ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(cause.errorResponse()).thenReturn(errorResponse);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(cause);
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient, PROPERTIES);

        Optional<StorageObject> result = service.get(new GetObjectCommand("attachments/missing.png"));

        assertTrue(result.isEmpty());
    }

    /**
     * 验证删除对象失败时会包装成稳定存储异常。
     */
    @Test
    @DisplayName("delete client failure wraps storage service exception")
    void delete_clientFailure_wrapsStorageServiceException() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        RuntimeException cause = new RuntimeException("delete failed");
        org.mockito.Mockito.doThrow(cause).when(minioClient).removeObject(any(RemoveObjectArgs.class));
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient, PROPERTIES);

        StorageServiceException exception = assertThrows(
                StorageServiceException.class,
                () -> service.delete(new DeleteObjectCommand("attachments/old.txt"))
        );

        assertEquals("failed to delete storage object: attachments/old.txt", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证生成预签名 URL 时会使用 GET 方法、对象键和 TTL 秒数。
     */
    @Test
    @DisplayName("create presigned url maps method object key and ttl")
    void createPresignedUrl_mapsMethodObjectKeyAndTtl() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.com/presigned/object");
        MinioObjectStorageService service = new MinioObjectStorageService(minioClient, PROPERTIES);

        PresignedUrl result = service.createPresignedUrl(new PresignedUrlCommand("attachments/report.pdf", Duration.ofMinutes(5)));

        ArgumentCaptor<GetPresignedObjectUrlArgs> argsCaptor = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(argsCaptor.capture());
        GetPresignedObjectUrlArgs args = argsCaptor.getValue();
        assertEquals(Method.GET, args.method());
        assertEquals("carrypigeon", args.bucket());
        assertEquals("attachments/report.pdf", args.object());
        assertEquals("https://example.com/presigned/object", result.url().toString());
        assertTrue(result.expiresAt().isAfter(Instant.now().plusSeconds(240)));
    }
}

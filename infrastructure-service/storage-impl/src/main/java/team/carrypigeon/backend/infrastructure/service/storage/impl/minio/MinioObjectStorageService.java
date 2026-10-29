package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.storage.api.exception.StorageServiceException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;
import team.carrypigeon.backend.infrastructure.service.storage.impl.config.MinioStorageProperties;

/**
 * MinIO 对象存储实现。
 * 职责：基于 MinIO 客户端实现 storage-api 定义的对象存储契约。
 * 边界：MinIO SDK 细节保持在 impl 内，不向上层暴露。
 */
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;
    private final TimeProviderImpl timeProvider;

    public MinioObjectStorageService(
            MinioClient minioClient,
            MinioStorageProperties properties,
            TimeProviderImpl timeProvider
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.timeProvider = timeProvider;
    }


    @Override
    public StorageObject put(String objectKey, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .stream(content, size, -1)
                            .contentType(contentType)
                            .build()
            );
            return StorageObject.metadata(objectKey, contentType, size);
        } catch (Exception ex) {
            throw new StorageServiceException("failed to put storage object: " + objectKey, ex);
        }
    }

    @Override
    public Optional<StorageObject> get(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );
            InputStream content = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );
            return Optional.of(new StorageObject(
                    objectKey,
                    stat.contentType(),
                    stat.size(),
                    Optional.of(content)
            ));
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equalsIgnoreCase(ex.errorResponse().code()) || "NoSuchObject".equalsIgnoreCase(ex.errorResponse().code())) {
                return Optional.empty();
            }
            throw new StorageServiceException("failed to read storage object: " + objectKey, ex);
        } catch (Exception ex) {
            throw new StorageServiceException("failed to read storage object: " + objectKey, ex);
        }
    }

    /**
     * 删除对象。
     * 约束：对象是否存在由 MinIO 自身处理，这里只做异常收口。
     */
    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageServiceException("failed to delete storage object: " + objectKey, ex);
        }
    }

    /**
     * 创建对象的预签名下载地址。
     * 输出：返回带绝对过期时间的下载 URL。
     */
    @Override
    public PresignedUrl createPresignedUrl(String objectKey, Duration ttlSeconds) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.bucket())
                            .object(objectKey)
                            .expiry((int) ttlSeconds.toSeconds())
                            .build()
            );
            return new PresignedUrl(java.net.URI.create(url), timeProvider.nowInstant().plus(ttlSeconds));
        } catch (Exception ex) {
            throw new StorageServiceException("failed to create presigned url for object: " + objectKey, ex);
        }
    }
}

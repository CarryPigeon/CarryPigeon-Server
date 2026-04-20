package team.carrypigeon.backend.infrastructure.service.storage.impl.minio;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.time.Instant;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.storage.api.exception.StorageServiceException;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
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

    public MinioObjectStorageService(MinioClient minioClient, MinioStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StorageObject put(PutObjectCommand command) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(command.objectKey())
                            .stream(command.content(), command.size(), -1)
                            .contentType(command.contentType())
                            .build()
            );
            return StorageObject.metadata(command.objectKey(), command.contentType(), command.size());
        } catch (Exception ex) {
            throw new StorageServiceException("failed to put storage object: " + command.objectKey(), ex);
        }
    }

    @Override
    public Optional<StorageObject> get(GetObjectCommand command) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(command.objectKey())
                            .build()
            );
            return Optional.of(StorageObject.withContent(
                    command.objectKey(),
                    stat.contentType(),
                    stat.size(),
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(properties.bucket())
                                    .object(command.objectKey())
                                    .build()
                    )
            ));
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equalsIgnoreCase(ex.errorResponse().code()) || "NoSuchObject".equalsIgnoreCase(ex.errorResponse().code())) {
                return Optional.empty();
            }
            throw new StorageServiceException("failed to read storage object: " + command.objectKey(), ex);
        } catch (Exception ex) {
            throw new StorageServiceException("failed to read storage object: " + command.objectKey(), ex);
        }
    }

    @Override
    public void delete(DeleteObjectCommand command) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(command.objectKey())
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageServiceException("failed to delete storage object: " + command.objectKey(), ex);
        }
    }

    @Override
    public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.bucket())
                            .object(command.objectKey())
                            .expiry((int) command.ttl().toSeconds())
                            .build()
            );
            return new PresignedUrl(java.net.URI.create(url), Instant.now().plus(command.ttl()));
        } catch (Exception ex) {
            throw new StorageServiceException("failed to create presigned url for object: " + command.objectKey(), ex);
        }
    }
}

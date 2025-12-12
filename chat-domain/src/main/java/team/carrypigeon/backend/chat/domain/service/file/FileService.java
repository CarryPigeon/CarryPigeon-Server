package team.carrypigeon.backend.chat.domain.service.file;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Upload file to MinIO only if an object with the same name and size does not exist.
     *
     * @param objectName  target object name in bucket
     * @param stream      file data stream
     * @param size        file size in bytes
     * @param contentType mime type, may be null
     * @return true if a new object is uploaded, false if a duplicate is detected
     */
    public boolean uploadIfNotExists(String objectName, InputStream stream, long size, String contentType) throws Exception {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            if (stat != null && stat.size() == size) {
                // object with same size already exists, treat as duplicate
                log.debug("FileService#uploadIfNotExists - duplicate detected, bucket={}, objectName={}, size={}", bucketName, objectName, size);
                return false;
            }
        } catch (ErrorResponseException e) {
            // 404 or 304 indicates object not found, continue to upload
            String code = e.errorResponse().code();
            if (!"NoSuchKey".equals(code) && !"NoSuchObject".equals(code)) {
                log.error("FileService#uploadIfNotExists - statObject unexpected error, bucket={}, objectName={}", bucketName, objectName, e);
                throw e;
            }
        }
        log.debug("FileService#uploadIfNotExists - uploading, bucket={}, objectName={}, size={}", bucketName, objectName, size);
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(stream, size, -1);
        if (contentType != null && !contentType.isEmpty()) {
            builder.contentType(contentType);
        }
        minioClient.putObject(builder.build());
        return true;
    }

    /**
     * Upload file to MinIO without any de-duplication logic.
     */
    public void upload(String objectName, InputStream stream, long size, String contentType) throws Exception {
        log.debug("FileService#upload - bucket={}, objectName={}, size={}", bucketName, objectName, size);
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(stream, size, -1);
        if (contentType != null && !contentType.isEmpty()) {
            builder.contentType(contentType);
        }
        minioClient.putObject(builder.build());
    }

    /**
     * Get basic metadata of an object.
     */
    public StatObjectResponse statFile(String objectName) throws Exception {
        log.debug("FileService#statFile - bucket={}, objectName={}", bucketName, objectName);
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Download file stream.
     */
    public InputStream downloadFile(String objectName) throws Exception {
        log.debug("FileService#downloadFile - bucket={}, objectName={}", bucketName, objectName);
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Delete file from bucket.
     */
    public void deleteFile(String objectName) throws Exception {
        log.debug("FileService#deleteFile - bucket={}, objectName={}", bucketName, objectName);
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }
}

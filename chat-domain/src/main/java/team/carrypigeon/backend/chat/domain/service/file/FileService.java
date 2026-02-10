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

/**
 * MinIO 文件对象服务。
 * <p>
 * 提供对象上传、下载、查询与删除能力，不承担权限判定与业务令牌校验。
 */
@Slf4j
@Service
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 构造文件服务。
     *
     * @param minioClient MinIO 客户端。
     */
    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 在对象不存在或大小不一致时上传文件。
     *
     * @param objectName 对象名。
     * @param stream 文件输入流。
     * @param size 文件大小（字节）。
     * @param contentType MIME 类型。
     * @return 上传了新对象返回 {@code true}，命中同尺寸对象返回 {@code false}。
     * @throws Exception 当 MinIO 访问失败时抛出。
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
                log.debug("FileService#uploadIfNotExists - duplicate detected, bucket={}, objectName={}, size={}", bucketName, objectName, size);
                return false;
            }
        } catch (ErrorResponseException e) {
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
     * 无去重策略上传对象。
     *
     * @param objectName 对象名。
     * @param stream 文件输入流。
     * @param size 文件大小（字节）。
     * @param contentType MIME 类型。
     * @throws Exception 当 MinIO 上传失败时抛出。
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
     * 查询对象元信息。
     *
     * @param objectName 对象名。
     * @return 对象元信息。
     * @throws Exception 当 MinIO 查询失败时抛出。
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
     * 下载对象文件流。
     *
     * @param objectName 对象名。
     * @return 文件输入流。
     * @throws Exception 当 MinIO 下载失败时抛出。
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
     * 删除对象。
     *
     * @param objectName 对象名。
     * @throws Exception 当 MinIO 删除失败时抛出。
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

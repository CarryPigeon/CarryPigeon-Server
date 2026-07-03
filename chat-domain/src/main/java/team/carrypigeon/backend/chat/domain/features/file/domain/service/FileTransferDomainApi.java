package team.carrypigeon.backend.chat.domain.features.file.domain.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.port.FileAttachmentAccessAuthorizer;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.projection.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 文件领域服务。
 * 职责：为上传申请、上传写入和下载访问提供最小编排能力。
 * 边界：不引入独立文件表，只依赖对象存储能力与稳定 share_key 规则。
 */
@Service
public class FileTransferDomainApi implements FileTransferApi {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(30);
    private static final long MAX_UPLOAD_SIZE_BYTES = 100L * 1024 * 1024;

    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final FileUploadShareKeyCodec uploadShareKeyCodec;
    private final FileObjectKeyResolver fileObjectKeyResolver;

    public FileTransferDomainApi(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            FileUploadShareKeyCodec uploadShareKeyCodec
    ) {
        this.objectStorageServiceProvider = objectStorageServiceProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
        this.fileObjectKeyResolver = new FileObjectKeyResolver(fileAttachmentAccessAuthorizer, uploadShareKeyCodec);
    }

    /**
     * 创建一次上传授权。
     * 职责：为客户端生成稳定 `share_key`、上传地址和过期时间。
     * 输入：文件名、声明的 MIME 类型和文件大小。
     * 输出：不带实际二进制内容的上传授权结果。
     * 约束：只校验上传前置条件，不写入对象存储。
     */
    public FileUploadGrantResult createUploadGrant(long accountId, String filename, String mimeType, long sizeBytes) {
        requirePositive(accountId, "accountId");
        validateUploadGrant(filename, mimeType, sizeBytes);
        long fileId = idGenerator.nextLongId();
        String shareKey = uploadShareKeyCodec.issue(accountId, fileId, sizeBytes);
        return new FileUploadGrantResult(
                fileId,
                shareKey,
                "/api/files/uploads/" + shareKey,
                timeProvider.nowInstant().plus(UPLOAD_URL_TTL)
        );
    }

    /**
     * 按 `share_key` 把二进制内容写入对象存储。
     * 输入：公开 `share_key`、内容类型、字节大小和内容流。
     * 副作用：会向对象存储写入真实对象。
     * 失败：当 `share_key`、内容流或大小非法时抛出校验异常。
     */
    public void uploadFile(long accountId, String shareKey, String contentType, long sizeBytes, java.io.InputStream content) {
        requirePositive(accountId, "accountId");
        if (shareKey == null || shareKey.isBlank()) {
            throw ProblemException.validationFailed("share_key must not be blank");
        }
        if (content == null) {
            throw ProblemException.validationFailed("file content must not be null");
        }
        FileObjectKeyResolver.ResolvedObject resolvedObject = fileObjectKeyResolver.resolveUploadObject(accountId, shareKey, sizeBytes);
        ObjectStorageService objectStorageService = requireObjectStorageService();
        objectStorageService.put(new PutObjectCommand(resolvedObject.objectKey(), content, sizeBytes, normalizeContentType(contentType)));
    }

    /**
     * 按 `share_key` 解析并读取文件下载结果。
     * 输出：文件存在时返回领域下载结果，缺失时返回空。
     * 边界：对象存储模型只在领域服务内部使用，不泄漏给协议入口。
     */
    public Optional<FileDownloadResult> downloadFile(Long accountId, String shareKey) {
        String objectKey = fileObjectKeyResolver.resolveDownloadObjectKey(accountId, shareKey);
        ObjectStorageService objectStorageService = requireObjectStorageService();
        Optional<StorageObject> storageObject = objectStorageService.get(new GetObjectCommand(objectKey));
        if (storageObject.isEmpty()) {
            return Optional.empty();
        }
        StorageObject object = storageObject.get();
        Optional<java.net.URI> redirectUrl = object.content().isPresent()
                ? Optional.empty()
                : Optional.of(objectStorageService.createPresignedUrl(new PresignedUrlCommand(objectKey, DOWNLOAD_URL_TTL)).url());
        return Optional.of(new FileDownloadResult(
                object.contentType(),
                object.size(),
                object.content(),
                redirectUrl
        ));
    }

    /**
     * 判断公开 `share_key` 是否映射为服务端头像对象。
     * 原因：服务端头像不走常规上传 key 规则，需要单独识别。
     */
    public boolean isServerAvatar(String shareKey) {
        return fileObjectKeyResolver.isServerAvatar(shareKey);
    }

    /**
     * 返回客户端上传时需要附带的固定请求头。
     * 输出：当前实现返回空集合，保留统一扩展点给未来存储策略。
     */
    public Map<String, String> uploadHeaders() {
        return Map.of();
    }

    private void validateUploadGrant(String filename, String mimeType, long sizeBytes) {
        if (filename == null || filename.isBlank()) {
            throw ProblemException.validationFailed("filename must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw ProblemException.validationFailed("mime_type must not be blank");
        }
        if (sizeBytes <= 0) {
            throw ProblemException.validationFailed("size_bytes must be greater than 0");
        }
        if (sizeBytes > MAX_UPLOAD_SIZE_BYTES) {
            throw ProblemException.validationFailed("size_bytes must be less than or equal to " + MAX_UPLOAD_SIZE_BYTES);
        }
    }

    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
    }
}

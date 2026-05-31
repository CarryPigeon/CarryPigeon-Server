package team.carrypigeon.backend.chat.domain.features.file.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.application.dto.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.support.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 文件应用服务。
 * 职责：为上传申请、上传写入和下载访问提供最小编排能力。
 * 边界：不引入独立文件表，只依赖对象存储能力与稳定 share_key 规则。
 */
@Service
public class FileApplicationService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);
    private static final String SERVER_AVATAR_SHARE_KEY = "server_avatar";
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public FileApplicationService(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider
    ) {
        this.objectStorageServiceProvider = objectStorageServiceProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    public FileUploadGrantResult createUploadGrant(String filename, String mimeType, long sizeBytes) {
        validateUploadGrant(filename, mimeType, sizeBytes);
        long fileId = idGenerator.nextLongId();
        String shareKey = "shr_" + fileId;
        return new FileUploadGrantResult(
                fileId,
                shareKey,
                "/api/files/uploads/" + shareKey,
                timeProvider.nowInstant().plus(UPLOAD_URL_TTL)
        );
    }

    public void uploadFile(String shareKey, String contentType, long sizeBytes, java.io.InputStream content) {
        if (shareKey == null || shareKey.isBlank()) {
            throw ProblemException.validationFailed("share_key must not be blank");
        }
        if (content == null) {
            throw ProblemException.validationFailed("file content must not be null");
        }
        if (sizeBytes < 0) {
            throw ProblemException.validationFailed("size_bytes must not be negative");
        }
        ObjectStorageService objectStorageService = requireObjectStorageService();
        objectStorageService.put(new PutObjectCommand(resolveObjectKey(shareKey), content, sizeBytes, normalizeContentType(contentType)));
    }

    public Optional<StorageObject> findStorageObject(String shareKey) {
        ObjectStorageService objectStorageService = requireObjectStorageService();
        return objectStorageService.get(new GetObjectCommand(resolveObjectKey(shareKey)));
    }

    public PresignedUrl createDownloadUrl(String shareKey) {
        ObjectStorageService objectStorageService = requireObjectStorageService();
        return objectStorageService.createPresignedUrl(new PresignedUrlCommand(resolveObjectKey(shareKey), Duration.ofMinutes(30)));
    }

    public boolean isServerAvatar(String shareKey) {
        return SERVER_AVATAR_SHARE_KEY.equals(shareKey);
    }

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
    }

    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    private String resolveObjectKey(String shareKey) {
        if (SERVER_AVATAR_SHARE_KEY.equals(shareKey)) {
            return SERVER_AVATAR_SHARE_KEY;
        }
        return FileShareKeyCodec.resolveObjectKey(shareKey);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
    }
}

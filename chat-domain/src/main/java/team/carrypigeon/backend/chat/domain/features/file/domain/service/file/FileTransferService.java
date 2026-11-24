package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.features.file.config.FileUploadProperties;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.CPFile;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileRepository;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.file.FileUploadShareKeyCodec.IssuedUploadShareKey;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 文件领域服务。
 * 职责：持久化授权、竞争上传权并发布不可变的完成结果。
 * 边界：对象上传不占用数据库事务；仅 COMPLETED 的元数据可以提供下载。
 */
@Slf4j
@Service
public class FileTransferService implements FileTransferApi {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);

    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final FileUploadShareKeyCodec uploadShareKeyCodec;
    private final FileRepository fileRepository;
    private final FileUploadProperties properties;

    public FileTransferService(
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
        IdGenerator idGenerator,
        TimeProvider timeProvider,
        FileUploadShareKeyCodec uploadShareKeyCodec,
        FileRepository uploads,
        FileUploadProperties properties
    ) {
        this.objectStorageServiceProvider = objectStorageServiceProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
        this.fileRepository = uploads;
        this.properties = properties;
    }

    @Override
    public FileUploadGrantResult createUploadGrant(long accountId, String fileName, String sha256, long sizeBytes) {
        // 数值校验
        if (sizeBytes > properties.maxFileBytes()) {
            throw ProblemException.of(FileProblem.FILE_TOO_LARGE);
        }
        if (sizeBytes <= 0) {
            throw ProblemException.of(FileProblem.INVALID_INPUT);
        }
        Optional<CPFile> sameFile = fileRepository.findBySha256(sha256);
        if (sameFile.isPresent()) {
            // 延期时间7天
            fileRepository.update(
                new CPFile(
                    sameFile.get().fileId(),
                    sameFile.get().ownerAccountId(),
                    sameFile.get().fileName(),
                    sameFile.get().shareKey(),
                    sameFile.get().sizeBytes(),
                    sameFile.get().status(),
                    sameFile.get().objectKey(),
                    sameFile.get().sha256(),
                    sameFile.get().contentType(),
                    sameFile.get().expiresAt().plus(Duration.ofDays(7)),
                    sameFile.get().completedAt()
                )
            );
            return new FileUploadGrantResult(
                sameFile.get().fileId(),
                sameFile.get().shareKey(),
                "/api/files/uploads/" + sameFile.get().shareKey(),
                sameFile.get().expiresAt().plus(Duration.ofDays(7)),
                true
            );
        }
        // 创建数据库数据
        long fileId = idGenerator.nextLongId();
        Instant expiresAt = timeProvider.nowInstant().plus(UPLOAD_URL_TTL);
        String shareKey = uploadShareKeyCodec.issue(accountId, fileId, sizeBytes, expiresAt);
        fileRepository.create(
            new CPFile(
                fileId,
                accountId,
                fileName,
                shareKey,
                sizeBytes,
                CPFile.Status.PENDING,
                null,
                sha256,
                null,
                expiresAt,
                null)
        );
        AuditLogs.success("file.upload_grant", accountId,
            "file_id", fileId, "size_bytes", sizeBytes);
        return new FileUploadGrantResult(
            fileId,
            shareKey,
            "/api/files/uploads/" + shareKey,
            expiresAt,
            false
        );
    }

    /**
     * 先在本地验证完整内容，再竞争写入权；重复完成请求只比较摘要，不再写对象。
     * 对象已写而数据库发布失败时保留 UPLOADING 状态与尝试登记，不能把对象误认为已完成。
     */
    @Override
    public void uploadFile(long accountId, String shareKey, String contentType, long sizeBytes, InputStream content) {
        // 原始 PUT 不受 multipart 限制，业务入口也执行上限，避免绕过 HTTP 申请入口。
        if (sizeBytes > properties.maxFileBytes()) {
            throw ProblemException.of(FileProblem.FILE_TOO_LARGE);
        }
        if (sizeBytes <= 0) {
            throw ProblemException.of(FileProblem.INVALID_INPUT);
        }
        try {
            IssuedUploadShareKey issued = uploadShareKeyCodec.parse(shareKey, timeProvider.nowInstant());
            if (issued.ownerAccountId() != accountId || issued.declaredSizeBytes() != sizeBytes) {
                throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
            }
            CPFile upload = fileRepository.findByShareKey(shareKey)
                .orElseThrow(() -> ProblemException.of(FileProblem.INVALID_SHARE_KEY));
            if (upload.ownerAccountId() != accountId || upload.sizeBytes() != sizeBytes) {
                throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
            }

            if (upload.status() == CPFile.Status.COMPLETED) {
                return;
            }

            String objectKey = "/files/objects/" + UUID.randomUUID();
            String storedType = contentType == null || contentType.isBlank()
                ? "application/octet-stream" : contentType.trim();
            requireStorage().put(objectKey, content, sizeBytes, storedType, upload.sha256());
            // 存储成功之后才发布数据库状态；这里失败时不回写 FAILED，避免掩盖不确定的提交结果。
            if (!fileRepository.complete(upload, objectKey, storedType, timeProvider.nowInstant())) {
                throw ProblemException.of(FileProblem.UPLOAD_CONFLICT);
            }
            AuditLogs.success("file.upload", accountId, "file_id", upload.fileId(), "size_bytes", sizeBytes);
        } catch (ProblemException exception) {
            AuditLogs.failure("file.upload", accountId, exception.mapping().message());
            throw exception;
        }
    }

    /**
     * 获取对象存储能力。
     * 说明：存储模块允许在部署时关闭；关闭后应用仍可启动，仅文件相关接口返回稳定错误。
     */
    private ObjectStorageService requireStorage() {
        ObjectStorageService storage = objectStorageServiceProvider.getIfAvailable();
        if (storage == null) {
            throw ProblemException.of(FileProblem.STORAGE_UNAVAILABLE);
        }
        return storage;
    }

    @Override
    public Optional<FileDownloadResult> downloadFile(String shareKey) {
        // 下载是登录后的持链分享；只检查发布状态，不复用上传签名的到期校验，也不绑定频道权限。
        CPFile upload = fileRepository.findByShareKey(shareKey).orElse(null);
        if (upload == null || upload.status() != CPFile.Status.COMPLETED) {
            return Optional.empty();
        }
        Optional<StorageObject> storageObject = requireStorage().get(upload.objectKey());
        if (storageObject.isEmpty()) {
            return Optional.empty();
        }
        StorageObject object = storageObject.get();
        return Optional.of(new FileDownloadResult(
            upload.fileName(),
            object.contentType(),
            object.size(),
            object.content().orElseThrow(() -> new IllegalStateException("Object storage returned no content stream"))
        ));
    }
}

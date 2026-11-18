package team.carrypigeon.backend.chat.domain.features.file.domain.service.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.file.config.FileUploadProperties;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileDownloadResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.file.result.FileUploadGrantResult;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.FileUpload;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileUploadRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.logging.AuditLogs;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;
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
    private final FileUploadRepository uploads;
    private final FileUploadProperties properties;
    private final TransactionRunner transactions;

    public FileTransferService(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            FileUploadShareKeyCodec uploadShareKeyCodec,
            FileUploadRepository uploads,
            FileUploadProperties properties,
            TransactionRunner transactions
    ) {
        this.objectStorageServiceProvider = objectStorageServiceProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.uploadShareKeyCodec = uploadShareKeyCodec;
        this.uploads = uploads;
        this.properties = properties;
        this.transactions = transactions;
    }

    @Override
    public FileUploadGrantResult createUploadGrant(long accountId, long sizeBytes) {
        if (sizeBytes > properties.maxFileBytes()) {
            throw ProblemException.of(FileProblem.FILE_TOO_LARGE);
        }
        if (sizeBytes <= 0) {
            throw ProblemException.of(FileProblem.INVALID_INPUT);
        }
        long fileId = idGenerator.nextLongId();
        Instant expiresAt = timeProvider.nowInstant().plus(UPLOAD_URL_TTL);
        String shareKey = uploadShareKeyCodec.issue(accountId, fileId, sizeBytes, expiresAt);
        transactions.runInTransaction(() -> {
            uploads.lockAccountQuota(accountId);
            requireQuota(accountId, sizeBytes, timeProvider.nowInstant());
            uploads.create(new FileUpload(fileId, accountId, shareKey, sizeBytes, FileUpload.Status.PENDING,
                    null, null, null, expiresAt, null));
        });
        AuditLogs.success("file.upload_grant", accountId,
                "file_id", fileId, "size_bytes", sizeBytes);
        return new FileUploadGrantResult(
                fileId,
                shareKey,
                "/api/files/uploads/" + shareKey,
                expiresAt
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
        Path staged = null;
        try {
            var issued = uploadShareKeyCodec.parse(shareKey, timeProvider.nowInstant());
            if (issued.ownerAccountId() != accountId || issued.declaredSizeBytes() != sizeBytes) {
                throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
            }
            FileUpload upload = uploads.findByShareKey(shareKey)
                    .orElseThrow(() -> ProblemException.of(FileProblem.INVALID_SHARE_KEY));
            if (upload.ownerAccountId() != accountId || upload.sizeBytes() != sizeBytes) {
                throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
            }

            staged = Files.createTempFile("cp-upload-", ".tmp");
            String sha256 = stageContent(content, staged, sizeBytes);
            if (upload.status() == FileUpload.Status.COMPLETED) {
                requireSameContent(upload, sha256);
                return;
            }

            String objectKey = "/files/objects/" + UUID.randomUUID();
            if (!claimUpload(upload, objectKey)) {
                // 另一个写入者可能刚完成；重新读取后允许内容相同的幂等重试。
                FileUpload current = uploads.findByShareKey(shareKey)
                        .orElseThrow(() -> ProblemException.of(FileProblem.INVALID_SHARE_KEY));
                requireSameContent(current, sha256);
                return;
            }
            String storedType = contentType == null || contentType.isBlank()
                    ? "application/octet-stream" : contentType.trim();
            try (InputStream stagedContent = Files.newInputStream(staged)) {
                requireStorage().put(objectKey, stagedContent, sizeBytes, storedType);
            } catch (IOException | RuntimeException exception) {
                try {
                    uploads.fail(upload.fileId(), objectKey, timeProvider.nowInstant());
                } catch (RuntimeException stateException) {
                    exception.addSuppressed(stateException);
                }
                throw exception;
            }
            // 存储成功之后才发布数据库状态；这里失败时不回写 FAILED，避免掩盖不确定的提交结果。
            if (!uploads.complete(upload.fileId(), objectKey, sha256, storedType, timeProvider.nowInstant())) {
                throw ProblemException.of(FileProblem.UPLOAD_CONFLICT);
            }
            AuditLogs.success("file.upload", accountId, "file_id", upload.fileId(), "size_bytes", sizeBytes);
        } catch (ProblemException exception) {
            AuditLogs.failure("file.upload", accountId, exception.reason());
            throw exception;
        } catch (IOException exception) {
            throw ProblemException.of(FileProblem.UPLOAD_READ_FAILED, exception);
        } finally {
            if (staged != null) {
                try {
                    Files.deleteIfExists(staged);
                } catch (IOException exception) {
                    log.warn("Failed to remove local upload staging file: {}", staged, exception);
                }
            }
        }
    }

    /** 失败后的重试需要重新预留；同账号申请和重试共享配额行锁，避免并发突破上限。 */
    private boolean claimUpload(FileUpload upload, String objectKey) {
        return transactions.runInTransaction(() -> {
            uploads.lockAccountQuota(upload.ownerAccountId());
            FileUpload current = uploads.findByShareKey(upload.shareKey())
                    .orElseThrow(() -> ProblemException.of(FileProblem.INVALID_SHARE_KEY));
            Instant now = timeProvider.nowInstant();
            if (!current.expiresAt().isAfter(now)) {
                throw ProblemException.of(FileProblem.INVALID_SHARE_KEY);
            }
            if (current.status() == FileUpload.Status.FAILED) {
                requireQuota(upload.ownerAccountId(), upload.sizeBytes(), now);
            }
            return uploads.claim(upload.fileId(), objectKey, now);
        });
    }

    /** 必须持有账号配额锁；通过状态表计算占用，不维护需要额外对账的加减计数器。 */
    private void requireQuota(long accountId, long sizeBytes, Instant now) {
        var usage = uploads.storageUsage(accountId, now);
        if (sizeBytes > properties.accountQuotaBytes() - usage.allocatedBytes()) {
            throw ProblemException.of(FileProblem.QUOTA_EXCEEDED);
        }
        if (usage.unfinishedUploads() >= properties.maxUnfinishedUploads()) {
            throw ProblemException.of(FileProblem.UPLOAD_LIMIT_REACHED);
        }
    }

    /** 验证实际字节数并生成服务端摘要；最多读取声明大小加一字节，不相信客户端提供的摘要。 */
    private String stageContent(InputStream content, Path staged, long expectedSize) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
        long received = 0;
        byte[] buffer = new byte[8192];
        try (var output = Files.newOutputStream(staged)) {
            while (true) {
                int limit = (int) Math.min(buffer.length, expectedSize - received + 1);
                int read = content.read(buffer, 0, limit);
                if (read == -1) {
                    break;
                }
                received += read;
                if (received > expectedSize) {
                    throw ProblemException.of(FileProblem.UPLOAD_SIZE_MISMATCH);
                }
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
            }
        }
        if (received != expectedSize) {
            throw ProblemException.of(FileProblem.UPLOAD_SIZE_MISMATCH);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** 上传中或内容不同均返回冲突；相同摘要保留原始完成结果，包括原 MIME 类型。 */
    private void requireSameContent(FileUpload upload, String sha256) {
        if (upload.status() != FileUpload.Status.COMPLETED || !sha256.equals(upload.sha256())) {
            throw ProblemException.of(FileProblem.UPLOAD_CONFLICT);
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
        FileUpload upload = uploads.findByShareKey(shareKey).orElse(null);
        if (upload == null || upload.status() != FileUpload.Status.COMPLETED) {
            return Optional.empty();
        }
        Optional<StorageObject> storageObject = requireStorage().get(upload.objectKey());
        if (storageObject.isEmpty()) {
            return Optional.empty();
        }
        StorageObject object = storageObject.get();
        return Optional.of(new FileDownloadResult(
                object.contentType(),
                object.size(),
                object.content().orElseThrow(() -> new IllegalStateException("Object storage returned no content stream"))
        ));
    }
}

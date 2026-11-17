package team.carrypigeon.backend.chat.domain.features.file.adaptor.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.FileStorageUsage;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.FileUpload;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileUploadRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileUploadDatabaseService;

/** 文件上传仓储适配器；仅负责领域状态与数据库快照转换，不处理对象存储。 */
@Component
public class DatabaseBackedFileUploadRepository implements FileUploadRepository {
    private final FileUploadDatabaseService database;

    public DatabaseBackedFileUploadRepository(FileUploadDatabaseService database) {
        this.database = database;
    }

    @Override
    public void lockAccountQuota(long accountId) {
        database.lockAccountQuota(accountId);
    }

    @Override
    public FileStorageUsage storageUsage(long accountId, Instant now) {
        var usage = database.storageUsage(accountId, now);
        return new FileStorageUsage(usage.allocatedBytes(), usage.unfinishedUploads());
    }

    @Override
    public void create(FileUpload upload) {
        database.create(new FileUploadRecord(upload.fileId(), upload.ownerAccountId(), upload.shareKey(),
                upload.sizeBytes(), upload.status().name(), upload.objectKey(), upload.sha256(),
                upload.contentType(), upload.expiresAt(), upload.completedAt()));
    }

    @Override
    public Optional<FileUpload> findByShareKey(String shareKey) {
        return database.findByShareKey(shareKey).map(this::toDomain);
    }

    private FileUpload toDomain(FileUploadRecord row) {
        return new FileUpload(row.fileId(), row.ownerAccountId(), row.shareKey(), row.sizeBytes(),
                FileUpload.Status.valueOf(row.status()), row.objectKey(), row.sha256(), row.contentType(),
                row.expiresAt(), row.completedAt());
    }

    @Override
    public Optional<FileUpload> findByIdForUpdate(long fileId) {
        return database.findByIdForUpdate(fileId).map(this::toDomain);
    }

    @Override
    public Optional<Long> avatarFileId(long accountId) {
        return database.avatarFileId(accountId);
    }

    @Override
    public void replaceAvatarReference(long accountId, Long fileId) {
        database.replaceAvatarReference(accountId, fileId);
    }

    @Override
    public void reclaimIfUnreferenced(long fileId, Instant now) {
        database.reclaimIfUnreferenced(fileId, now);
    }

    @Override
    public Optional<Long> findReclaimCandidateForUpdate(Instant now, Instant unusedBefore, Instant interruptedBefore) {
        return database.findReclaimCandidateForUpdate(now, unusedBefore, interruptedBefore);
    }

    @Override
    public void beginReclaim(long fileId, Instant now) {
        database.beginReclaim(fileId, now);
    }

    @Override
    public java.util.List<String> findObjectCleanupKeys(Instant now, int limit) {
        return database.findObjectCleanupKeys(now, limit);
    }

    @Override
    public void objectCleaned(String objectKey, Instant now, Instant nextCheck) {
        database.objectCleaned(objectKey, now, nextCheck);
    }

    @Override
    public void retryObjectCleanup(String objectKey, Instant retryAt) {
        database.retryObjectCleanup(objectKey, retryAt);
    }

    @Override
    public void finishReclaimedFiles(int limit) {
        database.finishReclaimedFiles(limit);
    }

    @Override
    public boolean claim(long fileId, String objectKey, Instant now) {
        return database.claim(fileId, objectKey, now);
    }

    @Override
    public boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt) {
        return database.complete(fileId, objectKey, sha256, contentType, completedAt);
    }

    @Override
    public void fail(long fileId, String objectKey, Instant now) {
        database.fail(fileId, objectKey, now);
    }
}

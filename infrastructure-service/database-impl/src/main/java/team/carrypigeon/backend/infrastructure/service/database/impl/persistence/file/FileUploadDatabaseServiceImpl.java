package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file;

import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileStorageUsageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileUploadDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file.FileUploadMapper;

/** 文件上传状态持久化；取得写入权和登记对象键原子提交，网络上传不占用数据库事务。 */
public class FileUploadDatabaseServiceImpl implements FileUploadDatabaseService {
    private final FileUploadMapper mapper;

    public FileUploadDatabaseServiceImpl(FileUploadMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAccountQuota(long accountId) {
        mapper.ensureQuotaAccount(accountId);
        mapper.lockQuotaAccount(accountId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FileStorageUsageRecord storageUsage(long accountId, Instant now) {
        return mapper.storageUsage(accountId, now);
    }

    @Override
    public void create(FileUploadRecord upload) {
        mapper.insert(upload);
    }

    @Override
    public Optional<FileUploadRecord> findByShareKey(String shareKey) {
        return Optional.ofNullable(mapper.selectByShareKey(shareKey));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(long fileId, String objectKey, Instant now) {
        if (mapper.claim(fileId, objectKey, now) == 0) {
            return false;
        }
        mapper.insertAttempt(fileId, objectKey, now);
        return true;
    }

    @Override
    public boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt) {
        return mapper.complete(fileId, objectKey, sha256, contentType, completedAt) == 1;
    }

    @Override
    @Transactional
    public void fail(long fileId, String objectKey, Instant now) {
        if (mapper.fail(fileId, objectKey) > 0) {
            mapper.retryObjectCleanup(objectKey, now);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<FileUploadRecord> findByIdForUpdate(long fileId) {
        return Optional.ofNullable(mapper.selectByIdForUpdate(fileId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> avatarFileId(long accountId) {
        return Optional.ofNullable(mapper.avatarFileId(accountId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void replaceAvatarReference(long accountId, Long fileId) {
        mapper.deleteAvatarReference(accountId);
        if (fileId != null) {
            mapper.insertAvatarReference(accountId, fileId);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reclaimIfUnreferenced(long fileId, Instant now) {
        if (mapper.reclaimIfUnreferenced(fileId, now) > 0) {
            mapper.scheduleObjects(fileId, now);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> findReclaimCandidateForUpdate(Instant now, Instant unusedBefore, Instant interruptedBefore) {
        return Optional.ofNullable(mapper.reclaimCandidate(now, unusedBefore, interruptedBefore));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void beginReclaim(long fileId, Instant now) {
        mapper.beginReclaim(fileId, now);
        mapper.scheduleObjects(fileId, now);
    }

    @Override
    public java.util.List<String> findObjectCleanupKeys(Instant now, int limit) {
        return mapper.objectCleanupKeys(now, limit);
    }

    @Override
    public void objectCleaned(String objectKey, Instant now, Instant nextCheck) {
        mapper.objectCleaned(objectKey, now, nextCheck);
    }

    @Override
    public void retryObjectCleanup(String objectKey, Instant retryAt) {
        mapper.retryObjectCleanup(objectKey, retryAt);
    }

    @Override
    public void finishReclaimedFiles(int limit) {
        mapper.finishReclaimedFiles(limit);
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileStorageUsageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileUploadDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.FileUploadMapper;

/** 文件上传状态持久化；取得写入权和登记对象键原子提交，网络上传不占用数据库事务。 */
@Component
public class MybatisFileUploadDatabaseService implements FileUploadDatabaseService {
    private final FileUploadMapper mapper;

    public MybatisFileUploadDatabaseService(FileUploadMapper mapper) {
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
    @Transactional
    public boolean claim(long fileId, String objectKey, Instant now) {
        if (mapper.claim(fileId, objectKey, now) == 0) {
            return false;
        }
        mapper.insertAttempt(fileId, objectKey);
        return true;
    }

    @Override
    public boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt) {
        return mapper.complete(fileId, objectKey, sha256, contentType, completedAt) == 1;
    }

    @Override
    public void fail(long fileId, String objectKey) {
        mapper.fail(fileId, objectKey);
    }
}

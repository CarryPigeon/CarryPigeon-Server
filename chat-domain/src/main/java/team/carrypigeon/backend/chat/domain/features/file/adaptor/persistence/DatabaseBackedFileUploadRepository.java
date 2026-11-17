package team.carrypigeon.backend.chat.domain.features.file.adaptor.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
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
    public void create(FileUpload upload) {
        database.create(new FileUploadRecord(upload.fileId(), upload.ownerAccountId(), upload.shareKey(),
                upload.sizeBytes(), upload.status().name(), upload.objectKey(), upload.sha256(),
                upload.contentType(), upload.expiresAt(), upload.completedAt()));
    }

    @Override
    public Optional<FileUpload> findByShareKey(String shareKey) {
        return database.findByShareKey(shareKey).map(row -> new FileUpload(row.fileId(), row.ownerAccountId(),
                row.shareKey(), row.sizeBytes(), FileUpload.Status.valueOf(row.status()), row.objectKey(),
                row.sha256(), row.contentType(), row.expiresAt(), row.completedAt()));
    }

    @Override
    public boolean claim(long fileId, String objectKey) {
        return database.claim(fileId, objectKey);
    }

    @Override
    public boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt) {
        return database.complete(fileId, objectKey, sha256, contentType, completedAt);
    }

    @Override
    public void fail(long fileId, String objectKey) {
        database.fail(fileId, objectKey);
    }
}

package team.carrypigeon.backend.chat.domain.features.file.adaptor.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.file.domain.model.CPFile;
import team.carrypigeon.backend.chat.domain.features.file.domain.repository.FileRepository;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.FileDatabaseService;

/** 文件上传仓储适配器；仅负责领域状态与数据库快照转换，不处理对象存储。 */
@Component
public class DatabaseBackedFileRepository implements FileRepository {
    private final FileDatabaseService database;

    public DatabaseBackedFileRepository(FileDatabaseService database) {
        this.database = database;
    }

    @Override
    public void create(CPFile upload) {
        database.insert(
            new FileRecord(
                upload.fileId(),
                upload.ownerAccountId(),
                upload.fileName(),
                upload.shareKey(),
                upload.sizeBytes(),
                upload.status().name(),
                upload.objectKey(),
                upload.sha256(),
                upload.contentType(),
                upload.expiresAt(),
                upload.completedAt()
            )
        );
    }

    @Override
    public Optional<CPFile> findById(long id) {
        return database.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CPFile> findByShareKey(String shareKey) {
        return database.findByShareKey(shareKey).map(this::toDomain);
    }

    @Override
    public Optional<CPFile> findBySha256(String sha256) {
        return database.findBySha256(sha256).map(this::toDomain);
    }

    @Override
    public List<CPFile> findExpired() {
        return List.of();
    }

    private CPFile toDomain(FileRecord row) {
        return new CPFile(
            row.fileId(),
            row.ownerAccountId(),
            row.fileName(),
            row.shareKey(),
            row.sizeBytes(),
            CPFile.Status.valueOf(row.status()),
            row.objectKey(),
            row.sha256(),
            row.contentType(),
            row.expiresAt(),
            row.completedAt()
        );
    }

    @Override
    public boolean complete(CPFile cpFile, String objectKey, String contentType, Instant completedAt) {
        return database.update(
            new FileRecord(
                cpFile.fileId(),
                cpFile.ownerAccountId(),
                cpFile.fileName(),
                cpFile.shareKey(),
                cpFile.sizeBytes(),
                cpFile.status().name(),
                objectKey,
                cpFile.sha256(),
                contentType,
                cpFile.expiresAt(),
                completedAt
            )
        );
    }

    @Override
    public boolean update(CPFile cpFile) {
        return database.update(
            new FileRecord(
                cpFile.fileId(),
                cpFile.ownerAccountId(),
                cpFile.fileName(),
                cpFile.shareKey(),
                cpFile.sizeBytes(),
                cpFile.status().name(),
                cpFile.objectKey(),
                cpFile.sha256(),
                cpFile.contentType(),
                cpFile.expiresAt(),
                cpFile.completedAt()
            )
        );
    }

    @Override
    public boolean delete(long fileId) {
        return database.delete(fileId);
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {
    long fileId;
    long ownerAccountId;
    String fileName;
    String shareKey;
    long sizeBytes;
    String status;
    String objectKey;
    String sha256;
    String contentType;
    Instant expiresAt;
    Instant completedAt;

    public FileRecord toFileRecord() {
        return new FileRecord(
            this.fileId,
            this.ownerAccountId,
            this.fileName,
            this.shareKey,
            this.sizeBytes,
            this.status,
            this.objectKey,
            this.sha256,
            this.contentType,
            this.expiresAt,
            this.completedAt
        );
    }

    public static FileEntity fromFileRecord(FileRecord fileRecord) {
        return new FileEntity(
            fileRecord.fileId(),
            fileRecord.ownerAccountId(),
            fileRecord.fileName(),
            fileRecord.shareKey(),
            fileRecord.sizeBytes(),
            fileRecord.status(),
            fileRecord.objectKey(),
            fileRecord.sha256(),
            fileRecord.contentType(),
            fileRecord.expiresAt(),
            fileRecord.completedAt()
        );
    }
}

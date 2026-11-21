package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/** 文件上传持久化快照；objectKey/sha256/contentType/completedAt 在对应阶段前可为 null。 */
public record FileRecord(
    long fileId,
    long ownerAccountId,
    String fileName,
    String shareKey,
    long sizeBytes,
    String status,
    String objectKey,
    String sha256,
    String contentType,
    Instant expiresAt,
    Instant completedAt
) {}

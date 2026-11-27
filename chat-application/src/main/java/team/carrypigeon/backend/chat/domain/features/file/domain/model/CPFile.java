package team.carrypigeon.backend.chat.domain.features.file.domain.model;

import java.time.Instant;

/**
 * 上传授权及其发布结果。文件仅在 COMPLETED 时可下载；完成后的对象键、摘要和类型不可变。
 * objectKey 在上传开始时即持久化，供中断后的回收使用；未开始时为 null。
 */
public record CPFile(
    long fileId,
    long ownerAccountId,
    String fileName,
    String shareKey,
    long sizeBytes,
    Status status,
    String objectKey,
    String sha256,
    String contentType,
    Instant expiresAt,
    Instant completedAt
) {
    /** PENDING/FAILED 可重新申请写入权；UPLOADING 只允许持有当前对象键的写入者完成。 */
    public enum Status { PENDING, UPLOADING, COMPLETED, FAILED, DELETING, DELETED }
}

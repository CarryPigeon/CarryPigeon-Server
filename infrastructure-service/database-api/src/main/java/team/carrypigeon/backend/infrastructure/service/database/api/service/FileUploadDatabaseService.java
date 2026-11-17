package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileStorageUsageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;

/** 文件上传的数据库契约；写入权与对象尝试登记必须在同一事务中提交。 */
public interface FileUploadDatabaseService {
    /** 以文件行锁读取状态；必须在事务内调用，与回收竞争时由行锁决定先后。 */
    Optional<FileUploadRecord> findByIdForUpdate(long fileId);

    /** 当前头像引用，未使用本地文件时为空；调用方须已锁定用户资料行。 */
    Optional<Long> avatarFileId(long accountId);

    /** 替换头像引用，null 表示解除；须先按 ID 升序锁定新旧文件。 */
    void replaceAvatarReference(long accountId, Long fileId);

    /** 没有任何头像引用时撤下完成文件，并安排其对象回收；与引用变更在同一事务内。 */
    void reclaimIfUnreferenced(long fileId, Instant now);

    /** 锁定一个到期候选：完成未引用 7 天，或授权过期（上传中另加中断宽限）。 */
    Optional<Long> findReclaimCandidateForUpdate(Instant now, Instant unusedBefore, Instant interruptedBefore);

    /** 将持锁文件撤下并安排对象清理；调用方必须已验证候选条件。 */
    void beginReclaim(long fileId, Instant now);

    /** 一批已知、非有效完成对象的待清理键；不扫描或删除未知历史对象。 */
    java.util.List<String> findObjectCleanupKeys(Instant now, int limit);

    /** 成功删除后登记清理时间，并保留低频复查，以覆盖跨系统写入的不确定结果。 */
    void objectCleaned(String objectKey, Instant now, Instant nextCheck);

    /** 删除失败只延期，不伪造已清理结果或释放已完成文件的配额。 */
    void retryObjectCleanup(String objectKey, Instant retryAt);

    /** 所有登记对象均在撤下后确认删除，才完成回收并释放已完成文件占用。 */
    void finishReclaimedFiles(int limit);

    /** 创建并锁定账号配额行；必须在业务事务内调用，锁持有到事务结束。 */
    void lockAccountQuota(long accountId);

    /** 读取已完成占用与有效预留；须在账号配额锁内读取。now 为当前时刻。 */
    FileStorageUsageRecord storageUsage(long accountId, Instant now);

    /** 保存授权；签名不能代替持久化的上传状态。 */
    void create(FileUploadRecord upload);

    /** 按完整 share key 查询；不存在返回空。 */
    Optional<FileUploadRecord> findByShareKey(String shareKey);

    /** 原子取得写入权并登记独立对象键；须持有账号配额锁，只允许未过期的 PENDING/FAILED。 */
    boolean claim(long fileId, String objectKey, Instant now);

    /** 仅当前写入者可发布完成结果；返回 false 表示状态已不允许完成。 */
    boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt);

    /** 记录当前尝试失败；保留对象登记，不能因失败丢失需要回收的对象键。 */
    void fail(long fileId, String objectKey, Instant now);
}

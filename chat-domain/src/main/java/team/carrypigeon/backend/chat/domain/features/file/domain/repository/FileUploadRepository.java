package team.carrypigeon.backend.chat.domain.features.file.domain.repository;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.FileStorageUsage;
import java.util.Optional;
import team.carrypigeon.backend.chat.domain.features.file.domain.model.FileUpload;

/** 文件上传状态仓储；状态切换直接访问持久化数据，不使用最终一致缓存。 */
public interface FileUploadRepository {
    /** 创建并锁定账号配额行；必须在业务事务内调用，锁持有到事务结束。 */
    void lockAccountQuota(long accountId);

    /** 读取已完成占用与有效预留；须在账号配额锁内读取。now 为当前时刻。 */
    FileStorageUsage storageUsage(long accountId, Instant now);

    /** 保存授权；签名不能代替持久化的上传状态。 */
    void create(FileUpload upload);

    /** 按完整 share key 查询；不存在返回空。 */
    Optional<FileUpload> findByShareKey(String shareKey);

    /** 原子取得写入权并登记独立对象键；只有 PENDING/FAILED 可取得，成功后事务已提交。 */
    boolean claim(long fileId, String objectKey, Instant now);

    /** 仅当前写入者可发布完成结果；返回 false 表示状态已不允许完成。 */
    boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt);

    /** 记录当前尝试失败；保留对象登记，不能因失败丢失需要回收的对象键。 */
    void fail(long fileId, String objectKey);
}

package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import java.util.Optional;
import team.carrypigeon.backend.infrastructure.service.database.api.model.FileUploadRecord;

/** 文件上传的数据库契约；写入权与对象尝试登记必须在同一事务中提交。 */
public interface FileUploadDatabaseService {
    /** 保存授权；签名不能代替持久化的上传状态。 */
    void create(FileUploadRecord upload);

    /** 按完整 share key 查询；不存在返回空。 */
    Optional<FileUploadRecord> findByShareKey(String shareKey);

    /** 原子取得写入权并登记独立对象键；只有 PENDING/FAILED 可取得，成功后事务已提交。 */
    boolean claim(long fileId, String objectKey);

    /** 仅当前写入者可发布完成结果；返回 false 表示状态已不允许完成。 */
    boolean complete(long fileId, String objectKey, String sha256, String contentType, Instant completedAt);

    /** 记录当前尝试失败；保留对象登记，不能因失败丢失需要回收的对象键。 */
    void fail(long fileId, String objectKey);
}

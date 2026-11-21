package team.carrypigeon.backend.chat.domain.features.file.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import team.carrypigeon.backend.chat.domain.features.file.domain.model.CPFile;

/** 文件上传状态仓储；状态切换直接访问持久化数据，不使用最终一致缓存。 */
public interface FileRepository {

    /**
     * 保存授权；签名不能代替持久化的上传状态。
     */
    void create(CPFile upload);

    /**
     * 根据 ID 查询上传状态。
     */
    Optional<CPFile> findById(long id);

    /**
     * 根据分享密钥查询上传状态。
     */
    Optional<CPFile> findByShareKey(String shareKey);

    /**
     * 根据 SHA-256 查询上传状态。
     */
    Optional<CPFile> findBySha256(String sha256);

    /**
     * 查询过期的上传状态。
     */
    List<CPFile> findExpired();

    /** 仅当前写入者可发布完成结果；返回 false 表示状态已不允许完成。 */
    boolean complete(CPFile cpFile, String objectKey, String contentType, Instant completedAt);

    /**
     * 更新上传状态。
     */
    boolean update(CPFile cpFile);

    /**
     * 删除上传状态。
     */
    boolean delete(long fileId);
}

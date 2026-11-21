package team.carrypigeon.backend.infrastructure.service.database.api.service;

import team.carrypigeon.backend.infrastructure.service.database.api.model.FileRecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 文件上传的数据库契约；写入权与对象尝试登记必须在同一事务中提交。 */
public interface FileDatabaseService {

    /**
     * 持久化存储文件信息
     * @param upload 文件信息
     * @return 是否成功
     * */
    boolean insert(FileRecord upload);

    /**
     * 删除文件上传记录。
     * @param id 文件上传记录ID
     * @return 是否成功
     * */
    boolean delete(long id);

    /**
     * 删除文件上传记录。
     * @param ids 文件上传记录ID
     * @return 是否成功
     * */
    boolean deleteByIds(Collection<Long> ids);

    /**
     * 根据ID查找文件上传记录。
     * @param id 文件上传记录ID
     * @return 文件上传记录
     * */
    Optional<FileRecord> findById(long id);

    /**
     * 根据ID查找文件上传记录。
     * @param ids 文件上传记录ID
     * @return 文件上传记录
     * */
    List<FileRecord> findByIds(Collection<Long> ids);

    /**
     * 根据分享密钥查找文件上传记录。
     * @param shareKey 分享密钥
     * @return 文件上传记录
     * */
    Optional<FileRecord> findByShareKey(String shareKey);

    /**
     * 根据SHA256查找文件上传记录。
     * @param sha256 SHA256
     * @return 文件上传记录
     * */
    Optional<FileRecord> findBySha256(String sha256);

    /**
     * 查找过期的文件上传记录。
     * @return 文件上传记录
     * */
    List<FileRecord> findExpired();

    /**
     * 更新文件上传记录。
     * @param record 文件上传记录
     * @return 是否成功
     * */
    boolean update(FileRecord record);

}

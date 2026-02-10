package team.carrypigeon.backend.api.dao.database.file;

import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;

import java.util.Collection;
import java.util.List;

/**
 * 文件元数据 DAO 接口。
 * <p>
 * 对应 `file_info` 表，负责文件元信息的查询与持久化。
 */
public interface FileInfoDao {

    /**
     * 按文件 ID 查询。
     *
     * @param id 文件 ID
     * @return 文件元信息，不存在时返回 null
     */
    CPFileInfo getById(long id);

    /**
     * 按 shareKey 查询。
     * <p>
     * `shareKey` 是对外下载接口中的稳定标识。
     *
     * @param shareKey 共享键
     * @return 文件元信息，不存在时返回 null
     */
    CPFileInfo getByShareKey(String shareKey);

    /**
     * 按 SHA-256 和大小查询。
     *
     * @param sha256 文件哈希
     * @param size   文件大小（字节）
     * @return 文件元信息，不存在时返回 null
     */
    CPFileInfo getBySha256AndSize(String sha256, long size);

    /**
     * 保存文件元数据（新增或更新）。
     *
     * @param fileInfo 文件元信息
     * @return 保存是否成功
     */
    boolean save(CPFileInfo fileInfo);

    /**
     * 按 ID 集合批量查询。
     *
     * @param ids 文件 ID 集合
     * @return 命中的文件列表
     */
    List<CPFileInfo> listByIds(Collection<Long> ids);
}

package team.carrypigeon.backend.api.dao.file;

import team.carrypigeon.backend.api.bo.domain.file.CPFileBO;

/**
 * 文件相关DAO接口
 * */
public interface CPFileDAO {
    /**
     * 通过文件id获取文件
     * */
    CPFileBO getFileById(Long id);
    /**
     * 通过sha256和文件大小获取文件
     * */
    CPFileBO getFileBySha256AndSize(String sha256, int size);
    /**
     * 添加文件
     */
    boolean addFile(CPFileBO fileBO);
    /**
     * 删除文件
     * */
    boolean deleteFile(Long id);
}

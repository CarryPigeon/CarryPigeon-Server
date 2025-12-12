package team.carrypigeon.backend.api.dao.database.file;

import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;

/**
 * File metadata DAO, used to access file_info table.
 */
public interface FileInfoDao {

    /**
     * Find file info by id.
     */
    CPFileInfo getById(long id);

    /**
     * Find file info by SHA-256 digest and size.
     */
    CPFileInfo getBySha256AndSize(String sha256, long size);

    /**
     * Save file info (insert new record).
     */
    boolean save(CPFileInfo fileInfo);
}

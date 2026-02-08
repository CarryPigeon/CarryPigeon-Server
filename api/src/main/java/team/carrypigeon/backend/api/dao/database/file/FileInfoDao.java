package team.carrypigeon.backend.api.dao.database.file;

import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;

import java.util.Collection;
import java.util.List;

/**
 * File metadata DAO, used to access file_info table.
 */
public interface FileInfoDao {

    /**
     * Find file info by id.
     */
    CPFileInfo getById(long id);

    /**
     * Find file info by share key.
     * <p>
     * Share key is the stable identifier exposed to clients via {@code /api/files/download/{share_key}}.
     */
    CPFileInfo getByShareKey(String shareKey);

    /**
     * Find file info by SHA-256 digest and size.
     */
    CPFileInfo getBySha256AndSize(String sha256, long size);

    /**
     * Save file info (insert or update).
     */
    boolean save(CPFileInfo fileInfo);

    /**
     * Batch load file infos by ids.
     * <p>
     * This is used by HTTP APIs to render avatar/download URLs without N+1 queries.
     */
    List<CPFileInfo> listByIds(Collection<Long> ids);
}

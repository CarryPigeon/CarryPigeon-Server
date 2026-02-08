package team.carrypigeon.backend.dao.database.impl.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoMapper;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoPO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link FileInfoDao} 的数据库实现（MyBatis-Plus）。
 * <p>
 * 文件元信息用于支撑 HTTP 文件上传/下载链路的去重与元数据查询。
 */
@Slf4j
@Service
public class FileInfoDaoImpl implements FileInfoDao {

    private final FileInfoMapper fileInfoMapper;

    /**
     * 创建文件信息 DAO 实现（由 Spring 注入 {@link FileInfoMapper}）。
     */
    public FileInfoDaoImpl(FileInfoMapper fileInfoMapper) {
        this.fileInfoMapper = fileInfoMapper;
    }

    @Override
    public CPFileInfo getById(long id) {
        log.debug("FileInfoDaoImpl#getById - id={}", id);
        CPFileInfo result = Optional.ofNullable(fileInfoMapper.selectById(id))
                .map(FileInfoPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("FileInfoDaoImpl#getById - file not found, id={}", id);
        }
        return result;
    }

    @Override
    public CPFileInfo getByShareKey(String shareKey) {
        if (shareKey == null || shareKey.isBlank()) {
            return null;
        }
        log.debug("FileInfoDaoImpl#getByShareKey - shareKey={}", shareKey);
        LambdaQueryWrapper<FileInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfoPO::getShareKey, shareKey);
        CPFileInfo result = Optional.ofNullable(fileInfoMapper.selectOne(queryWrapper))
                .map(FileInfoPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("FileInfoDaoImpl#getByShareKey - file not found, shareKey={}", shareKey);
        }
        return result;
    }

    @Override
    public CPFileInfo getBySha256AndSize(String sha256, long size) {
        log.debug("FileInfoDaoImpl#getBySha256AndSize - sha256={}, size={}", sha256, size);
        LambdaQueryWrapper<FileInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfoPO::getSha256, sha256)
                .eq(FileInfoPO::getSize, size);
        CPFileInfo result = Optional.ofNullable(fileInfoMapper.selectOne(queryWrapper))
                .map(FileInfoPO::toBo)
                .orElse(null);
        if (result == null) {
            log.debug("FileInfoDaoImpl#getBySha256AndSize - file not found, sha256={}, size={}", sha256, size);
        }
        return result;
    }

    @Override
    public boolean save(CPFileInfo fileInfo) {
        if (fileInfo == null) {
            log.error("FileInfoDaoImpl#save called with null fileInfo");
            return false;
        }
        boolean success = fileInfoMapper.insertOrUpdate(FileInfoPO.fromBo(fileInfo));
        if (success) {
            log.debug("FileInfoDaoImpl#save success, fileId={}", fileInfo.getId());
        } else {
            log.warn("FileInfoDaoImpl#save failed, fileId={}", fileInfo.getId());
        }
        return success;
    }

    @Override
    public List<CPFileInfo> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return fileInfoMapper.selectBatchIds(ids).stream()
                .map(FileInfoPO::toBo)
                .toList();
    }
}

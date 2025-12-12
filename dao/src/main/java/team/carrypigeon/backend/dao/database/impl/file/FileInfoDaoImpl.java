package team.carrypigeon.backend.dao.database.impl.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoMapper;
import team.carrypigeon.backend.dao.database.mapper.file.FileInfoPO;

import java.util.Optional;

@Slf4j
@Service
public class FileInfoDaoImpl implements FileInfoDao {

    private final FileInfoMapper fileInfoMapper;

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
        int affected = fileInfoMapper.insert(FileInfoPO.fromBo(fileInfo));
        boolean success = affected > 0;
        if (success) {
            log.debug("FileInfoDaoImpl#save success, fileId={}", fileInfo.getId());
        } else {
            log.warn("FileInfoDaoImpl#save failed, fileId={}", fileInfo.getId());
        }
        return success;
    }
}

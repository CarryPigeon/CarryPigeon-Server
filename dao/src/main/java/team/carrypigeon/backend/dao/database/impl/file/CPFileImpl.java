package team.carrypigeon.backend.dao.database.impl.file;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.bo.domain.file.CPFileBO;
import team.carrypigeon.backend.api.dao.file.CPFileDAO;
import team.carrypigeon.backend.dao.database.mapper.file.FileMapper;
import team.carrypigeon.backend.dao.database.mapper.file.FilePO;

@Component
public class CPFileImpl implements CPFileDAO {

    private final FileMapper fileMapper;

    public CPFileImpl(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public CPFileBO getFileById(Long id) {
        FilePO filePO = fileMapper.selectById(id);
        return filePO == null ? null : filePO.toBO();
    }

    @Override
    public CPFileBO getFileBySha256AndSize(String sha256, int size) {
        QueryWrapper<FilePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sha256", sha256).eq("size", size);
        FilePO filePO = fileMapper.selectOne(queryWrapper);
        return filePO == null ? null : filePO.toBO();
    }

    @Override
    public boolean addFile(CPFileBO fileBO) {
        FilePO filePO = new FilePO();
        filePO.fill(fileBO);
        return fileMapper.insert(filePO)>0;
    }

    @Override
    public boolean deleteFile(Long id) {
        return fileMapper.deleteById(id)>0;
    }
}

package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 文件状态 SQL */
@Mapper
public interface FileUploadMapper extends BaseMapper<FileEntity> { }

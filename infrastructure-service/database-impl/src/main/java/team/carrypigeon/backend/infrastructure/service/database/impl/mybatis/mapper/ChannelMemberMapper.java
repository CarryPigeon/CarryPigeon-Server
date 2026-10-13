package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelMemberEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelOwnerProjection;

/**
 * 频道成员 Mapper。
 * 职责：提供 chat_channel_member 表的显式 SQL 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface ChannelMemberMapper extends BaseMapper<ChannelMemberEntity> {}

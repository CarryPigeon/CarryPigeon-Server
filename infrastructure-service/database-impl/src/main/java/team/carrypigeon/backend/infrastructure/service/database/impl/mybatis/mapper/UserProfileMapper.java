package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.UserProfileEntity;

/**
 * 用户资料 Mapper。
 * 职责：提供 user_profile 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileEntity> {
}

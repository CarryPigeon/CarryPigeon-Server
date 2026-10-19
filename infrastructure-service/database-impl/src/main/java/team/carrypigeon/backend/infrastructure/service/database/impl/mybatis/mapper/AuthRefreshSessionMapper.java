package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;

/**
 * 刷新会话 Mapper。
 * 职责：提供 auth_refresh_session 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；撤销语义由服务层用条件更新表达。
 */
@Mapper
public interface AuthRefreshSessionMapper extends BaseMapper<AuthRefreshSessionEntity> {
}

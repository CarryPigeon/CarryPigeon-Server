package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;

/**
 * 刷新会话 Mapper。
 * 职责：提供 auth_refresh_session 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface AuthRefreshSessionMapper extends BaseMapper<AuthRefreshSessionEntity> {

    /**
     * 撤销刷新会话并更新时间。
     *
     * @param id 会话 ID
     * @return 受影响行数
     */
    @Update("""
            UPDATE auth_refresh_session
            SET revoked = true, updated_at = CURRENT_TIMESTAMP(6)
            WHERE id = #{id}
            """)
    int revokeById(long id);
}

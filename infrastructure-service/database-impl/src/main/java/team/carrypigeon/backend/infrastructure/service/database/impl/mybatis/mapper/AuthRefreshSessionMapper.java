package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.AuthRefreshSessionEntity;

/**
 * 刷新会话 Mapper。
 * 职责：提供 auth_refresh_session 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；撤销语义由服务层用条件更新表达。
 */
@Mapper
public interface AuthRefreshSessionMapper extends BaseMapper<AuthRefreshSessionEntity> {

    /**
     * 单条 SQL 批量插入刷新会话。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO auth_refresh_session (
                id, account_id, refresh_token_hash, expires_at, revoked, created_at, updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.id},
                #{record.accountId},
                #{record.refreshTokenHash},
                #{record.expiresAt},
                #{record.revoked},
                #{record.createdAt},
                #{record.updatedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<AuthRefreshSessionEntity> records);
}

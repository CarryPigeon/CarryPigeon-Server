package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelBanEntity;

/**
 * 频道封禁 Mapper。
 * 职责：提供 chat_channel_ban 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；insertAll 是通用批量写入元能力。
 */
@Mapper
public interface ChannelBanMapper extends BaseMapper<ChannelBanEntity> {

    /**
     * 单条 SQL 批量插入频道封禁记录。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel_ban (
                ban_id, channel_id, banned_account_id, operator_account_id,
                reason, expires_at, created_at, revoked_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.banId},
                #{record.channelId},
                #{record.bannedAccountId},
                #{record.operatorAccountId},
                #{record.reason},
                #{record.expiresAt},
                #{record.createdAt},
                #{record.revokedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelBanEntity> records);
}

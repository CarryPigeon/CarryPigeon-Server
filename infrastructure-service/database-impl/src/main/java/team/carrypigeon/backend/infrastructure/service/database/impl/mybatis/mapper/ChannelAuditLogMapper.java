package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelAuditLogEntity;

/**
 * 频道审计日志 Mapper。
 * 职责：提供 chat_channel_audit_log 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
public interface ChannelAuditLogMapper extends BaseMapper<ChannelAuditLogEntity> {

    @Select("""
            <script>
            SELECT audit_id, channel_id, actor_account_id, action_type, metadata, created_at
            FROM chat_channel_audit_log
            WHERE 1 = 1
            <if test="cursorAuditId != null">
              AND audit_id &lt; #{cursorAuditId}
            </if>
            <if test="channelId != null">
              AND channel_id = #{channelId}
            </if>
            <if test="actorAccountId != null">
              AND actor_account_id = #{actorAccountId}
            </if>
            <if test="actionType != null and actionType != ''">
              AND action_type = #{actionType}
            </if>
            <if test="fromTime != null">
              AND created_at &gt;= #{fromTime}
            </if>
            <if test="toTime != null">
              AND created_at &lt;= #{toTime}
            </if>
            ORDER BY audit_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChannelAuditLogEntity> list(
            @Param("cursorAuditId") Long cursorAuditId,
            @Param("limit") int limit,
            @Param("channelId") Long channelId,
            @Param("actorAccountId") Long actorAccountId,
            @Param("actionType") String actionType,
            @Param("fromTime") java.time.Instant fromTime,
            @Param("toTime") java.time.Instant toTime
    );
}

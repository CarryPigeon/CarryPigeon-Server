package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelDiscoverProjection;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelEntity;

/**
 * 频道 Mapper。
 * 职责：提供 chat_channel 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
public interface ChannelMapper extends BaseMapper<ChannelEntity> {

    @Select("""
            <script>
            SELECT
                c.id,
                c.conversation_id,
                c.name,
                c.brief,
                c.avatar,
                c.type,
                c.is_default,
                COUNT(cm.account_id) AS member_count,
                CASE WHEN c.type = 'private' THEN TRUE ELSE FALSE END AS requires_application,
                c.created_at,
                c.updated_at
            FROM chat_channel c
            LEFT JOIN chat_channel_member cm ON cm.channel_id = c.id
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
                AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.brief LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="cursorChannelId != null">
                AND c.id &lt; #{cursorChannelId}
            </if>
            <if test="type != null and type != ''">
                AND c.type = #{type}
            </if>
            GROUP BY c.id, c.conversation_id, c.name, c.brief, c.avatar, c.type, c.is_default, c.created_at, c.updated_at
            ORDER BY c.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChannelDiscoverProjection> discoverChannels(
            @Param("keyword") String keyword,
            @Param("cursorChannelId") Long cursorChannelId,
            @Param("type") String type,
            @Param("limit") int limit
    );
}

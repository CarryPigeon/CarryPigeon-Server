package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelUnreadProjection;

/**
 * 频道已读状态 Mapper。
 * 职责：提供 chat_channel_read_state 表的读写入口。
 * 边界：保留的显式 SQL 只有 UPSERT、条件推进和未读聚合，
 * 这三类语义（ON DUPLICATE KEY、集合聚合）无法用 MyBatis-Plus 条件构造器表达。
 */
@Mapper
public interface ChannelReadStateMapper extends BaseMapper<ChannelReadStateEntity> {

    /**
     * 新增或覆盖频道已读状态。
     *
     * @param entity 已读状态实体
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO chat_channel_read_state (
                channel_id,
                account_id,
                last_read_message_id,
                last_read_time,
                created_at,
                updated_at
            ) VALUES (
                #{channelId},
                #{accountId},
                #{lastReadMessageId},
                #{lastReadTime},
                #{createdAt},
                #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                last_read_message_id = VALUES(last_read_message_id),
                last_read_time = VALUES(last_read_time),
                updated_at = VALUES(updated_at)
            """)
    int upsertState(ChannelReadStateEntity entity);

    /**
     * 仅当新锚点更靠后时推进已读状态。
     *
     * @param entity 已读状态实体
     * @return 受影响行数；0 表示新锚点不比已存锚点更靠后
     */
    @Update("""
            INSERT INTO chat_channel_read_state (
                channel_id, account_id, last_read_message_id, last_read_time, created_at, updated_at
            ) VALUES (
                #{channelId}, #{accountId}, #{lastReadMessageId}, #{lastReadTime}, #{createdAt}, #{updatedAt}
            ) ON DUPLICATE KEY UPDATE
                last_read_time = IF(VALUES(last_read_message_id) > last_read_message_id, VALUES(last_read_time), last_read_time),
                updated_at = IF(VALUES(last_read_message_id) > last_read_message_id, VALUES(updated_at), updated_at),
                last_read_message_id = IF(VALUES(last_read_message_id) > last_read_message_id, VALUES(last_read_message_id), last_read_message_id)
            """)
    int advanceIfNewer(ChannelReadStateEntity entity);

    /**
     * 聚合账户在各频道的未读消息数与最后已读时间。
     *
     * @param accountId 账户 ID
     * @return 未读投影列表
     */
    @Select("""
            <script>
            SELECT
                cm.channel_id AS channelId,
                COUNT(m.message_id) AS unreadCount,
                COALESCE(r.last_read_time, c.created_at) AS lastReadTime
            FROM chat_channel_member cm
            JOIN chat_channel c ON c.id = cm.channel_id
            LEFT JOIN chat_channel_read_state r
                   ON r.channel_id = cm.channel_id AND r.account_id = cm.account_id
            LEFT JOIN chat_message m
                  ON m.channel_id = cm.channel_id
                  AND m.message_id &gt; COALESCE(r.last_read_message_id, 0)
                  AND m.sender_id != cm.account_id
                  AND m.status != 'recalled'
            WHERE cm.account_id = #{accountId}
            GROUP BY cm.channel_id, r.last_read_time, c.created_at
            HAVING COUNT(m.message_id) &gt; 0
            ORDER BY cm.channel_id ASC
            </script>
            """)
    List<ChannelUnreadProjection> findUnreadsByAccountId(@Param("accountId") long accountId);
}

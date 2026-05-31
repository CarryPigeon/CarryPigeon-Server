package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;

/**
 * 频道已读状态 Mapper。
 */
public interface ChannelReadStateMapper extends BaseMapper<ChannelReadStateEntity> {

    @Select("""
            SELECT channel_id, account_id, last_read_message_id, last_read_time, created_at, updated_at
            FROM chat_channel_read_state
            WHERE channel_id = #{channelId}
              AND account_id = #{accountId}
            LIMIT 1
            """)
    ChannelReadStateEntity findByChannelIdAndAccountId(@Param("channelId") long channelId, @Param("accountId") long accountId);

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
            WHERE cm.account_id = #{accountId}
            GROUP BY cm.channel_id, r.last_read_time, c.created_at
            HAVING COUNT(m.message_id) &gt; 0
            ORDER BY cm.channel_id ASC
            </script>
            """)
    List<UnreadProjection> listUnreadsByAccountId(@Param("accountId") long accountId);

    interface UnreadProjection {
        long getChannelId();
        long getUnreadCount();
        java.time.Instant getLastReadTime();
    }
}

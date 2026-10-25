package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelReadStateEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelUnreadProjection;

/**
 * 频道已读状态 Mapper。
 * 职责：提供 chat_channel_read_state 表的读写入口。
 * 边界：显式 SQL 只保留 UPSERT 和未读读模型聚合；条件推进规则由 Repository 组合。
 */
@Mapper
public interface ChannelReadStateMapper extends BaseMapper<ChannelReadStateEntity> {

    /**
     * 单条 SQL 批量插入已读状态记录。
     *
     * @param records 待插入实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_channel_read_state (
                channel_id,
                account_id,
                last_read_message_id,
                last_read_time,
                created_at,
                updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.channelId},
                #{record.accountId},
                #{record.lastReadMessageId},
                #{record.lastReadTime},
                #{record.createdAt},
                #{record.updatedAt}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("records") List<ChannelReadStateEntity> records);

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
     * 以行锁读取指定账户在频道中的已读状态。
     * 约束：必须在事务内调用。
     *
     * @param channelId 频道 ID
     * @param accountId 账户 ID
     * @return 命中时返回实体，未命中时返回 null
     */
    @Select("""
            SELECT channel_id, account_id, last_read_message_id, last_read_time, created_at, updated_at
            FROM chat_channel_read_state
            WHERE channel_id = #{channelId} AND account_id = #{accountId}
            FOR UPDATE
            """)
    ChannelReadStateEntity selectByRelationForUpdate(
            @Param("channelId") long channelId,
            @Param("accountId") long accountId
    );

    /**
     * 按复合主键批量查询。
     *
     * @param keys 复合主键列表
     * @return 命中的实体列表
     */
    @Select("""
            <script>
            SELECT channel_id, account_id, last_read_message_id, last_read_time, created_at, updated_at
            FROM chat_channel_read_state
            WHERE (channel_id, account_id) IN
            <foreach collection="keys" item="key" open="(" separator="," close=")">
              (#{key.channelId}, #{key.accountId})
            </foreach>
            </script>
            """)
    List<ChannelReadStateEntity> selectByKeys(@Param("keys") List<ChannelReadStateKey> keys);

    /**
     * 按复合主键批量删除。
     *
     * @param keys 复合主键列表
     * @return 受影响行数
     */
    @Delete("""
            <script>
            DELETE FROM chat_channel_read_state
            WHERE (channel_id, account_id) IN
            <foreach collection="keys" item="key" open="(" separator="," close=")">
              (#{key.channelId}, #{key.accountId})
            </foreach>
            </script>
            """)
    int deleteByKeys(@Param("keys") List<ChannelReadStateKey> keys);

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
            JOIN chat_channel c ON c.channel_id = cm.channel_id
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

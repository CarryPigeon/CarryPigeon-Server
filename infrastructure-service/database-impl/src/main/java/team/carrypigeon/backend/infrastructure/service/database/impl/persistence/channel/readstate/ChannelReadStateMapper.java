package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.channel.readstate;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import team.carrypigeon.backend.infrastructure.service.database.api.model.ChannelReadStateKey;

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
                last_read_sequence,
                created_at,
                updated_at
            )
            VALUES
            <foreach collection="records" item="record" separator=",">
              (
                #{record.channelId},
                #{record.accountId},
                #{record.lastReadMessageId},
                #{record.lastReadSequence},
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
                last_read_sequence,
                created_at,
                updated_at
            ) VALUES (
                #{channelId},
                #{accountId},
                #{lastReadMessageId},
                #{lastReadSequence},
                #{createdAt},
                #{updatedAt}
            )
            ON DUPLICATE KEY UPDATE
                last_read_message_id = VALUES(last_read_message_id),
                last_read_sequence = VALUES(last_read_sequence),
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
            SELECT channel_id, account_id, last_read_message_id, last_read_sequence, created_at, updated_at
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
            SELECT channel_id, account_id, last_read_message_id, last_read_sequence, created_at, updated_at
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

    /** 账户当前可见频道及其已读边界；无已读记录时从序号 0 开始。 */
    record UnreadBoundary(long channelId, long lastReadSequence) {}

    /** 先读取各频道的边界，使后续计数可以使用常量范围索引，而非逐行过滤已读历史。 */
    @Select("""
            SELECT cm.channel_id, COALESCE(r.last_read_sequence, 0) AS last_read_sequence
            FROM chat_channel_member cm
            JOIN chat_channel c ON c.channel_id = cm.channel_id
            LEFT JOIN chat_channel_read_state r
                ON r.channel_id = cm.channel_id AND r.account_id = cm.account_id
            WHERE cm.account_id = #{accountId}
            ORDER BY cm.channel_id ASC
            """)
    List<UnreadBoundary> findUnreadBoundaries(@Param("accountId") long accountId);

    /**
     * 对一批频道分别以常量边界做索引范围查询，各自最多计数 10000 条有效未读。
     * 两次查询之间成员关系可能已删除，因此统计时再次校验频道与成员关系。
     */
    @Select("""
            <script>
            <foreach collection="boundaries" item="boundary" separator=" UNION ALL ">
                SELECT #{boundary.channelId} AS channelId, (
                    SELECT COUNT(*) FROM (
                        SELECT 1 FROM chat_message m FORCE INDEX (idx_message_unread)
                        WHERE m.channel_id = #{boundary.channelId}
                          AND m.status = 'sent'
                          AND m.sequence &gt; #{boundary.lastReadSequence}
                          AND m.sender_id != #{accountId}
                        LIMIT 10000
                    ) limited_unreads
                ) AS unreadCount
                WHERE EXISTS (
                    SELECT 1 FROM chat_channel_member cm
                    JOIN chat_channel c ON c.channel_id = cm.channel_id
                    WHERE cm.account_id = #{accountId} AND cm.channel_id = #{boundary.channelId}
                )
            </foreach>
            </script>
            """)
    List<ChannelUnreadProjection> countUnreads(
            @Param("accountId") long accountId,
            @Param("boundaries") List<UnreadBoundary> boundaries
    );
}

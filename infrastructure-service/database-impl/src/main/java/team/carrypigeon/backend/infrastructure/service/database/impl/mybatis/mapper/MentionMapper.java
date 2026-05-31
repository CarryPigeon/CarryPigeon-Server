package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MentionEntity;

/**
 * 提及 Mapper。
 */
public interface MentionMapper {

    @Insert("""
            INSERT INTO chat_mention (
                mention_id,
                channel_id,
                message_id,
                from_account_id,
                target_type,
                target_account_id,
                created_at,
                is_read
            )
            VALUES (
                #{mentionId},
                #{channelId},
                #{messageId},
                #{fromAccountId},
                #{targetType},
                #{targetAccountId},
                #{createdAt},
                #{read}
            )
            """)
    int insert(MentionEntity entity);

    @Select("""
            <script>
            SELECT mention_id, channel_id, message_id, from_account_id, target_type, target_account_id, created_at, is_read
            FROM chat_mention
            WHERE target_account_id = #{accountId}
            <if test="cursorMentionId != null">
              AND mention_id &lt; #{cursorMentionId}
            </if>
            <if test="unreadOnly">
              AND is_read = FALSE
            </if>
            <if test="channelId != null">
              AND channel_id = #{channelId}
            </if>
            ORDER BY mention_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<MentionEntity> listByAccountId(
            @Param("accountId") long accountId,
            @Param("cursorMentionId") Long cursorMentionId,
            @Param("limit") int limit,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("channelId") Long channelId
    );

    @Update("""
            UPDATE chat_mention
            SET is_read = TRUE
            WHERE mention_id = #{mentionId}
              AND target_account_id = #{accountId}
              AND is_read = FALSE
            """)
    int markAsRead(@Param("accountId") long accountId, @Param("mentionId") long mentionId);

    @Update("""
            <script>
            UPDATE chat_mention
            SET is_read = TRUE
            WHERE target_account_id = #{accountId}
              AND is_read = FALSE
            <if test="beforeMentionId != null">
              AND mention_id &lt;= #{beforeMentionId}
            </if>
            <if test="channelId != null">
              AND channel_id = #{channelId}
            </if>
            </script>
            """)
    int markAllAsRead(
            @Param("accountId") long accountId,
            @Param("beforeMentionId") Long beforeMentionId,
            @Param("channelId") Long channelId
    );
}

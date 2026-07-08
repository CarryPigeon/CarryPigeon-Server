package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.ChannelPinEntity;

/**
 * 频道置顶 Mapper。
 */
@Mapper
public interface ChannelPinMapper {

    @Select("""
            SELECT pin_id, channel_id, message_id, pinned_by_account_id, note, pinned_at
            FROM chat_channel_pin
            WHERE channel_id = #{channelId} AND message_id = #{messageId}
            LIMIT 1
            """)
    ChannelPinEntity findByChannelIdAndMessageId(@Param("channelId") long channelId, @Param("messageId") long messageId);

    @Insert("""
            INSERT INTO chat_channel_pin (pin_id, channel_id, message_id, pinned_by_account_id, note, pinned_at)
            VALUES (#{pinId}, #{channelId}, #{messageId}, #{pinnedByAccountId}, #{note}, #{pinnedAt})
            """)
    int insert(ChannelPinEntity entity);

    @Delete("""
            DELETE FROM chat_channel_pin
            WHERE channel_id = #{channelId} AND message_id = #{messageId}
            """)
    int delete(@Param("channelId") long channelId, @Param("messageId") long messageId);

    @Delete("""
            DELETE FROM chat_channel_pin
            WHERE message_id = #{messageId}
            """)
    int deleteByMessageId(@Param("messageId") long messageId);

    @Select("""
            <script>
            SELECT pin_id, channel_id, message_id, pinned_by_account_id, note, pinned_at
            FROM chat_channel_pin
            WHERE channel_id = #{channelId}
            <if test="cursorMessageId != null">
              AND message_id &lt; #{cursorMessageId}
            </if>
            ORDER BY message_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChannelPinEntity> findByChannelIdBefore(
            @Param("channelId") long channelId,
            @Param("cursorMessageId") Long cursorMessageId,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(*)
            FROM chat_channel_pin
            WHERE channel_id = #{channelId}
            """)
    long countByChannelId(@Param("channelId") long channelId);
}

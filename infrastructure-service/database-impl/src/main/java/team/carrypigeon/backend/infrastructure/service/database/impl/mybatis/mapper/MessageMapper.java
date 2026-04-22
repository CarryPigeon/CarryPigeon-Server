package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;

/**
 * 消息 Mapper。
 * 职责：提供 chat_message 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    /**
     * 按频道查询指定游标之前的历史消息。
     *
     * @param channelId 频道 ID
     * @param cursorMessageId 游标消息 ID，可为空
     * @param limit 查询条数
     * @return 消息实体列表
     */
    @Select("""
            <script>
            SELECT message_id, server_id, conversation_id, channel_id, sender_id, message_type,
                   body, preview_text, searchable_text, payload, metadata, status, created_at
            FROM chat_message
            WHERE channel_id = #{channelId}
            <if test="cursorMessageId != null">
                AND message_id <![CDATA[ < ]]> #{cursorMessageId}
            </if>
            ORDER BY message_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<MessageEntity> findByChannelIdBefore(
            @Param("channelId") long channelId,
            @Param("cursorMessageId") Long cursorMessageId,
            @Param("limit") int limit
    );

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param channelId 频道 ID
     * @param keyword 搜索关键字
     * @param limit 返回条数
     * @return 搜索命中消息列表
     */
    @Select("""
            SELECT message_id, server_id, conversation_id, channel_id, sender_id, message_type,
                   body, preview_text, searchable_text, payload, metadata, status, created_at
            FROM chat_message
            WHERE channel_id = #{channelId}
              AND searchable_text LIKE CONCAT('%', #{keyword}, '%')
            ORDER BY message_id DESC
            LIMIT #{limit}
            """)
    List<MessageEntity> searchByChannelId(
            @Param("channelId") long channelId,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );
}

package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;

/**
 * 消息 Mapper。
 * 职责：提供 canonical chat_message 表的查询与撤回更新入口。
 * 边界：仅供 database-impl 内部服务使用。
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    @Select("""
            <script>
            SELECT message_id, sender_id, channel_id, domain, domain_version,
                   data, send_time, mentions, preview, status
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

    @Select("""
            SELECT message_id, sender_id, channel_id, domain, domain_version,
                   data, send_time, mentions, preview, status
            FROM chat_message
            WHERE channel_id = #{channelId}
              AND message_id <![CDATA[ > ]]> #{afterMessageId}
            ORDER BY message_id ASC
            LIMIT #{limit}
            """)
    List<MessageEntity> findByChannelIdAfter(
            @Param("channelId") long channelId,
            @Param("afterMessageId") long afterMessageId,
            @Param("limit") int limit
    );

    @Select("""
            SELECT message_id, sender_id, channel_id, domain, domain_version,
                   data, send_time, mentions, preview, status
            FROM chat_message
            WHERE channel_id = #{channelId}
              AND status = 'sent'
              AND (preview LIKE CONCAT('%', #{keyword}, '%')
                   OR CAST(data AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY message_id DESC
            LIMIT #{limit}
            """)
    List<MessageEntity> searchByChannelId(
            @Param("channelId") long channelId,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT message_id, sender_id, channel_id, domain, domain_version,
                   data, send_time, mentions, preview, status
            FROM chat_message
            WHERE channel_id = #{channelId}
              AND status = 'sent'
              AND (preview LIKE CONCAT('%', #{keyword}, '%')
                   OR CAST(data AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))
            <if test="cursorMessageId != null">
              AND message_id <![CDATA[ < ]]> #{cursorMessageId}
            </if>
            <if test="senderAccountId != null">
              AND sender_id = #{senderAccountId}
            </if>
            <if test="domain != null and domain != ''">
              AND domain = #{domain}
            </if>
            <if test="beforeMessageId != null">
              AND message_id <![CDATA[ < ]]> #{beforeMessageId}
            </if>
            <if test="afterMessageId != null">
              AND message_id <![CDATA[ > ]]> #{afterMessageId}
            </if>
            ORDER BY message_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<MessageEntity> searchByChannelIdWithFilters(
            @Param("channelId") long channelId,
            @Param("keyword") String keyword,
            @Param("cursorMessageId") Long cursorMessageId,
            @Param("senderAccountId") Long senderAccountId,
            @Param("domain") String domain,
            @Param("beforeMessageId") Long beforeMessageId,
            @Param("afterMessageId") Long afterMessageId,
            @Param("limit") int limit
    );

    @Update("""
            UPDATE chat_message
            SET data = #{data},
                mentions = #{mentions},
                preview = #{preview},
                status = #{status}
            WHERE message_id = #{messageId}
            """)
    int updateMessage(MessageEntity entity);
}

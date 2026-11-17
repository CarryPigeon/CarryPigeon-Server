package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageSendRequestRecord;

/**
 * 消息 Mapper。
 * 职责：提供 canonical chat_message 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；批量 insertAll 是通用多行写入元能力。
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

    @Delete("DELETE FROM chat_message_sequence WHERE channel_id = #{channelId}")
    void deleteSequence(@Param("channelId") long channelId);

    @Delete("DELETE FROM chat_message WHERE channel_id = #{channelId} LIMIT #{limit}")
    int deleteMessagesBatch(@Param("channelId") long channelId, @Param("limit") int limit);

    @Delete("DELETE FROM chat_message_send_request WHERE channel_id = #{channelId} LIMIT #{limit}")
    int deleteSendRequestsBatch(@Param("channelId") long channelId, @Param("limit") int limit);

    @Delete("DELETE FROM chat_channel_read_state WHERE channel_id = #{channelId} LIMIT #{limit}")
    int deleteReadStatesBatch(@Param("channelId") long channelId, @Param("limit") int limit);

    @Delete("DELETE FROM chat_mention WHERE channel_id = #{channelId} LIMIT #{limit}")
    int deleteMentionsBatch(@Param("channelId") long channelId, @Param("limit") int limit);

    /** 唯一键把同一发送者、频道和请求 ID 的并发调用串行化；重复请求不得覆盖原摘要。 */
    @Insert("""
            INSERT INTO chat_message_send_request (account_id, channel_id, request_id, payload_hash, message_id)
            VALUES (#{accountId}, #{channelId}, #{requestId}, #{payloadHash}, #{messageId})
            ON DUPLICATE KEY UPDATE request_id = request_id
            """)
    int reserveSend(@Param("accountId") long accountId, @Param("channelId") long channelId,
            @Param("requestId") String requestId, @Param("payloadHash") String payloadHash, @Param("messageId") long messageId);

    /** 当前读取得首个已提交事务的结果，不受调用方此前建立的快照影响。 */
    @Select("""
            SELECT payload_hash, message_id FROM chat_message_send_request
            WHERE account_id = #{accountId} AND channel_id = #{channelId} AND request_id = #{requestId}
            FOR UPDATE
            """)
    MessageSendRequestRecord selectSendForUpdate(@Param("accountId") long accountId, @Param("channelId") long channelId,
            @Param("requestId") String requestId);

    /** 惰性创建序号行或递增；排他行锁由消息事务持有到提交或回滚。 */
    @Insert("""
            INSERT INTO chat_message_sequence (channel_id, last_sequence) VALUES (#{channelId}, 1)
            ON DUPLICATE KEY UPDATE last_sequence = last_sequence + 1
            """)
    int advanceSequence(@Param("channelId") long channelId);

    /** 使用当前读，避免 MySQL REPEATABLE READ 快照看不到刚等待提交的序号。 */
    @Select("SELECT last_sequence FROM chat_message_sequence WHERE channel_id = #{channelId} FOR UPDATE")
    long selectSequenceForUpdate(@Param("channelId") long channelId);

    /**
     * 单条 SQL 批量插入消息记录。
     *
     * @param messages 待插入消息实体
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO chat_message (
                message_id,
                sender_id,
                channel_id,
                sequence,
                domain,
                data,
                send_time,
                mentions,
                preview,
                status
            )
            VALUES
            <foreach collection="messages" item="message" separator=",">
              (
                #{message.messageId},
                #{message.senderId},
                #{message.channelId},
                #{message.sequence},
                #{message.domain},
                #{message.data},
                #{message.sendTime},
                #{message.mentions},
                #{message.preview},
                #{message.status}
              )
            </foreach>
            </script>
            """)
    int insertAll(@Param("messages") List<MessageEntity> messages);
}

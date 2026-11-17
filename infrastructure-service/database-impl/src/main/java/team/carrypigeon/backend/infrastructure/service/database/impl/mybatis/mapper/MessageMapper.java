package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;

/**
 * 消息 Mapper。
 * 职责：提供 canonical chat_message 表的 MyBatis-Plus 访问入口。
 * 边界：仅供 database-impl 内部服务使用；批量 insertAll 是通用多行写入元能力。
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {

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

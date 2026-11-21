package team.carrypigeon.backend.infrastructure.service.database.impl.persistence.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;

/**
 * 消息 canonical 持久化实体。
 * 职责：承接 chat_message 的统一消息字段与 MyBatis-Plus 映射。
 * 边界：data 与 mentions 保持 JSON 文本，不解释 domain 业务语义。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_message")
public class MessageEntity {

    @TableId(value = "message_id", type = IdType.INPUT)
    private Long messageId;
    private Long senderId;
    private Long channelId;
    private String domain;
    private String data;
    private Instant sendTime;
    private String mentions;
    private String preview;
    private String searchText;
    private String status;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public MessageRecord toRecord() {
        return new MessageRecord(
                java.util.Objects.requireNonNull(messageId, "messageId"),
                java.util.Objects.requireNonNull(senderId, "senderId"),
                java.util.Objects.requireNonNull(channelId, "channelId"),
                domain,
                data,
                sendTime,
                mentions,
                preview,
                searchText,
                status
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 消息数据库记录契约
     * @return 对应持久化实体
     */
    public static MessageEntity from(MessageRecord record) {
        return new MessageEntity(
                record.messageId(),
                record.senderId(),
                record.channelId(),
                record.domain(),
                record.data(),
                record.sendTime(),
                record.mentions(),
                record.preview(),
                record.searchText(),
                record.status()
        );
    }
}

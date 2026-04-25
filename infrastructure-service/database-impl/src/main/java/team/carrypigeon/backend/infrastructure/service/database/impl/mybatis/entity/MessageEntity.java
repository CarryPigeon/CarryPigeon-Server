package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 消息持久化实体。
 * 职责：承接 chat_message 表字段与 MyBatis-Plus 映射。
 * 边界：仅供 database-impl 内部使用，不暴露到 database-api。
 */
@TableName("chat_message")
@Data
public class MessageEntity {

    @TableId(value = "message_id", type = IdType.INPUT)
    private Long messageId;
    private String serverId;
    private Long conversationId;
    private Long channelId;
    private Long senderId;
    private String messageType;
    private String body;
    private String previewText;
    private String searchableText;
    private String payload;
    private String metadata;
    private String status;
    private Instant createdAt;
}

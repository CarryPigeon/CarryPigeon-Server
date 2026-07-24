package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;

/**
 * 消息 canonical 持久化实体。
 * 职责：承接 chat_message 的十个统一字段与 MyBatis-Plus 映射。
 * 边界：data 与 mentions 保持 JSON 文本，不解释 domain 业务语义。
 */
@TableName("chat_message")
@Data
public class MessageEntity {

    @TableId(value = "message_id", type = IdType.INPUT)
    private Long messageId;
    private Long senderId;
    private Long channelId;
    private String domain;
    private String domainVersion;
    private String data;
    private Instant sendTime;
    private String mentions;
    private String preview;
    private String status;
}

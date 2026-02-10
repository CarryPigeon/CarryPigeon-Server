package team.carrypigeon.backend.dao.database.mapper.message;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;

import java.time.LocalDateTime;

/**
 * `message` 表持久化对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("message")
public class MessagePO {

    /**
     * 消息 ID。
     */
    @TableId
    private Long id;

    /**
     * 发送者用户 ID。
     */
    private Long uid;

    /**
     * 频道 ID。
     */
    private Long cid;

    /**
     * 消息领域。
     */
    private String domain;

    /**
     * 消息领域版本。
     */
    private String domainVersion;

    /**
     * 回复目标消息 ID。
     */
    @TableField("reply_to_mid")
    private Long replyToMid;

    /**
     * 序列化后的消息数据。
     */
    private String data;

    /**
     * 发送时间。
     */
    private LocalDateTime sendTime;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 PO 转换为 BO。
     *
     * @return 消息领域对象。
     */
    @SneakyThrows
    public CPMessage toBo() {
        return new CPMessage()
                .setId(id)
                .setUid(uid)
                .setCid(cid)
                .setDomain(domain)
                .setDomainVersion(domainVersion)
                .setReplyToMid(replyToMid == null ? 0L : replyToMid)
                .setData(objectMapper.readValue(data, JsonNode.class))
                .setSendTime(sendTime);
    }

    /**
     * 从 BO 构建 PO。
     *
     * @param message 消息领域对象。
     * @return 消息持久化对象。
     */
    @SneakyThrows
    public static MessagePO fromBo(CPMessage message) {
        return new MessagePO()
                .setId(message.getId())
                .setUid(message.getUid())
                .setCid(message.getCid())
                .setDomain(message.getDomain())
                .setDomainVersion(message.getDomainVersion())
                .setReplyToMid(message.getReplyToMid() <= 0 ? 0L : message.getReplyToMid())
                .setData(objectMapper.writeValueAsString(message.getData()))
                .setSendTime(message.getSendTime());
    }
}

package team.carrypigeon.backend.dao.database.mapper.message;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 数据库中消息的映射类
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("message")
public class MessagePO {
    // 消息id
    @TableId
    private Long id;
    // 用户id
    private Long uid;
    // 通道id
    private Long cid;
    // 消息域，格式为 Domain:SubDomain
    private String domain;
    // 消息域版本
    private String domainVersion;
    // 回复目标消息 id（mid），0 表示非回复
    @TableField("reply_to_mid")
    private Long replyToMid;
    // 消息数据
    private String data;
    // 发送时间
    private LocalDateTime sendTime;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    /**
     * 将当前 PO 转换为领域对象（BO）。
     */
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

    @SneakyThrows
    /**
     * 从领域对象（BO）创建 PO。
     */
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

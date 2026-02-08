package team.carrypigeon.backend.api.bo.domain.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据库中消息的映射类
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPMessage {
    // 消息id
    private long id;
    // 用户id
    private long uid;
    // 通道id
    private long cid;
    // 消息域，格式为 Domain:SubDomain
    private String domain;
    // 消息域版本（SemVer 字符串，例如 1.0.0）
    private String domainVersion;
    /**
     * Reply target message id (mid), 0 means not a reply.
     * <p>
     * JSON protocol uses {@code reply_to_mid} as a snowflake string.
     */
    private long replyToMid;
    // 消息数据
    private JsonNode data;
    // 发送时间
    private LocalDateTime sendTime;
}

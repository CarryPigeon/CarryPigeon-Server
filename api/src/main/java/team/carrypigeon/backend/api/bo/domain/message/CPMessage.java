package team.carrypigeon.backend.api.bo.domain.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息领域对象。
 * <p>
 * 对应消息持久化实体，记录发送者、频道、领域与消息载荷。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPMessage {

    /**
     * 消息 ID。
     */
    private long id;

    /**
     * 发送者用户 ID。
     */
    private long uid;

    /**
     * 频道 ID。
     */
    private long cid;

    /**
     * 消息领域，格式为 `Domain:SubDomain`。
     */
    private String domain;

    /**
     * 消息领域版本（SemVer）。
     */
    private String domainVersion;

    /**
     * 回复目标消息 ID，`0` 表示非回复消息。
     */
    private long replyToMid;

    /**
     * 消息结构化载荷。
     */
    private JsonNode data;

    /**
     * 发送时间。
     */
    private LocalDateTime sendTime;
}

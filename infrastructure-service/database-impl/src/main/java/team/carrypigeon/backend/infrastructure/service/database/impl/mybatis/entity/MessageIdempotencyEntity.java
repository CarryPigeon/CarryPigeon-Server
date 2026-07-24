package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import java.time.Instant;
import lombok.Data;

/**
 * 消息写操作幂等持久化实体。
 * 边界：只映射 chat_message_idempotency，不承载消息业务规则。
 */
@Data
public class MessageIdempotencyEntity {
    private Long accountId;
    private String operation;
    private String idempotencyKey;
    private String requestFingerprint;
    private Long messageId;
    private Instant createdAt;
    private Instant completedAt;
}

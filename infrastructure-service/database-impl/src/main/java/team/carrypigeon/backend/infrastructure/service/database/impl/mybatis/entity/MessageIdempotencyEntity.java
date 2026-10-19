package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;

/**
 * 消息写操作幂等持久化实体。
 * 职责：承接 chat_message_idempotency 表字段与 MyBatis-Plus 映射。
 * 边界：复合唯一键语义由 mapper 的显式 SQL 承担，本实体不定义主键。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_message_idempotency")
public class MessageIdempotencyEntity {

    private Long accountId;
    private String operation;
    private String idempotencyKey;
    private String requestFingerprint;
    private Long messageId;
    private Instant createdAt;
    private Instant completedAt;

    /**
     * 将持久化实体转换为数据库记录契约。
     *
     * @return 对应数据库记录契约
     */
    public MessageIdempotencyRecord toRecord() {
        return new MessageIdempotencyRecord(
                accountId == null ? 0L : accountId,
                operation,
                idempotencyKey,
                requestFingerprint,
                messageId,
                createdAt,
                completedAt
        );
    }

    /**
     * 将数据库记录契约转换为持久化实体。
     *
     * @param record 消息幂等数据库记录契约
     * @return 对应持久化实体
     */
    public static MessageIdempotencyEntity from(MessageIdempotencyRecord record) {
        return new MessageIdempotencyEntity(
                record.accountId(),
                record.operation(),
                record.idempotencyKey(),
                record.requestFingerprint(),
                record.messageId(),
                record.createdAt(),
                record.completedAt()
        );
    }
}

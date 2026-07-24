package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper;

import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageIdempotencyEntity;

/**
 * 消息幂等 Mapper。
 * 职责：利用唯一键和行锁提供事务内的原子预留与结果绑定。
 * 边界：调用必须由上层 TransactionRunner 包裹，不能脱离事务组合使用。
 */
@Mapper
public interface MessageIdempotencyMapper {

    /**
     * 尝试插入预留；键已存在时执行无值变更更新并等待已有事务释放唯一键锁。
     */
    @Insert("""
            INSERT INTO chat_message_idempotency (
                account_id,
                operation,
                idempotency_key,
                request_fingerprint,
                message_id,
                created_at,
                completed_at
            )
            VALUES (
                #{accountId},
                #{operation},
                #{idempotencyKey},
                #{requestFingerprint},
                NULL,
                #{createdAt},
                NULL
            )
            ON DUPLICATE KEY UPDATE idempotency_key = VALUES(idempotency_key)
            """)
    int reserve(MessageIdempotencyEntity entity);

    /**
     * 锁定读取当前预留，保证检查与结果绑定之间没有竞争写入。
     */
    @Select("""
            SELECT account_id, operation, idempotency_key, request_fingerprint,
                   message_id, created_at, completed_at
            FROM chat_message_idempotency
            WHERE account_id = #{accountId}
              AND operation = #{operation}
              AND idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    MessageIdempotencyEntity findForUpdate(
            @Param("accountId") long accountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey
    );

    /**
     * 将首次成功创建的消息绑定到当前预留；指纹不一致或重复完成时不会更新。
     */
    @Update("""
            UPDATE chat_message_idempotency
            SET message_id = #{messageId},
                completed_at = #{completedAt}
            WHERE account_id = #{accountId}
              AND operation = #{operation}
              AND idempotency_key = #{idempotencyKey}
              AND request_fingerprint = #{requestFingerprint}
              AND message_id IS NULL
            """)
    int complete(
            @Param("accountId") long accountId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestFingerprint") String requestFingerprint,
            @Param("messageId") long messageId,
            @Param("completedAt") Instant completedAt
    );
}

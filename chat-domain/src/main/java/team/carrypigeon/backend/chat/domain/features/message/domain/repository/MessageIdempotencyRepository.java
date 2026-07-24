package team.carrypigeon.backend.chat.domain.features.message.domain.repository;

import java.time.Instant;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;

/**
 * 消息写操作幂等仓储。
 * 职责：在当前事务内原子预留幂等键、锁定既有结果并绑定最终消息。
 * 边界：并发控制由持久化适配器实现，仓储不改变 canonical message。
 */
public interface MessageIdempotencyRepository {

    /**
     * 原子创建或锁定幂等预留。
     * 输入：尚未绑定消息结果的预留信息。
     * 输出：当前键的持久化记录；已有请求会返回原指纹和结果消息 ID。
     *
     * @param reservation 待创建或获取的预留
     * @return 当前键的锁定记录
     */
    MessageIdempotency reserve(MessageIdempotency reservation);

    /**
     * 在当前事务内为预留绑定最终消息结果。
     *
     * @param accountId 发起账号 ID
     * @param operation 操作命名空间
     * @param idempotencyKey 幂等键
     * @param requestFingerprint 请求指纹
     * @param messageId 最终消息 ID
     * @param completedAt 完成时间
     */
    void complete(
            long accountId,
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            long messageId,
            Instant completedAt
    );
}

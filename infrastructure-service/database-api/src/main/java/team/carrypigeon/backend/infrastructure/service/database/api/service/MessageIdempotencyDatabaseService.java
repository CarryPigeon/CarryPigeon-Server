package team.carrypigeon.backend.infrastructure.service.database.api.service;

import java.time.Instant;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageIdempotencyRecord;

/**
 * 消息幂等数据库服务抽象。
 * 职责：提供事务内原子预留、锁定读取和结果绑定能力。
 * 边界：调用方必须在同一数据库事务中完成预留、消息写入与结果绑定。
 */
public interface MessageIdempotencyDatabaseService {

    /**
     * 原子创建预留或锁定并读取已有预留。
     * 并发约束：相同账号、操作和幂等键的调用必须串行返回同一持久化记录。
     *
     * @param reservation 待创建的预留
     * @return 当前幂等键的锁定记录
     */
    MessageIdempotencyRecord reserve(MessageIdempotencyRecord reservation);

    /**
     * 为当前事务锁定的预留绑定消息结果。
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

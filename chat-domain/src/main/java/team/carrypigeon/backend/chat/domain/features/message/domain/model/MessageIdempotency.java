package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

/**
 * 消息写操作幂等预留。
 * 职责：绑定账号、操作、幂等键、请求指纹与最终消息结果。
 * 边界：该模型独立于 canonical message，不进入消息 Wire 或 domain data。
 *
 * @param accountId 发起操作的账号 ID
 * @param operation 幂等键所在的操作命名空间
 * @param idempotencyKey 客户端幂等键
 * @param requestFingerprint 规范化请求指纹
 * @param messageId 已完成操作产生的消息 ID；预留阶段为空
 * @param createdAt 预留创建时间
 * @param completedAt 结果绑定时间；预留阶段为空
 */
public record MessageIdempotency(
        long accountId,
        String operation,
        String idempotencyKey,
        String requestFingerprint,
        Long messageId,
        Instant createdAt,
        Instant completedAt
) {
}

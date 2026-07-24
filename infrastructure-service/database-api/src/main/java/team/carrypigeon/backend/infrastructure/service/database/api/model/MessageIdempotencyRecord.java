package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 消息幂等数据库记录契约。
 * 职责：表达消息写操作原子预留和最终结果绑定所需的最小持久化字段。
 * 边界：不解释请求指纹，不复制消息协议或业务内容。
 *
 * @param accountId 发起操作的账号 ID
 * @param operation 操作命名空间
 * @param idempotencyKey 客户端幂等键
 * @param requestFingerprint 规范化请求指纹
 * @param messageId 最终消息 ID；预留阶段为空
 * @param createdAt 创建时间
 * @param completedAt 完成时间；预留阶段为空
 */
public record MessageIdempotencyRecord(
        long accountId,
        String operation,
        String idempotencyKey,
        String requestFingerprint,
        Long messageId,
        Instant createdAt,
        Instant completedAt
) {
}

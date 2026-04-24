package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道审计日志领域模型。
 * 职责：表达频道治理相关事件的最小追加式留痕记录。
 * 边界：这里只提供持久化基础，不扩展为独立审计子系统。
 *
 * @param auditId 审计记录 ID
 * @param channelId 频道 ID
 * @param actorAccountId 操作人账户 ID；系统触发时允许为空
 * @param actionType 操作类型
 * @param targetAccountId 目标账户 ID；无目标账户时允许为空
 * @param metadata 结构化扩展数据
 * @param createdAt 创建时间
 */
public record ChannelAuditLog(
        long auditId,
        long channelId,
        Long actorAccountId,
        String actionType,
        Long targetAccountId,
        String metadata,
        Instant createdAt
) {
}

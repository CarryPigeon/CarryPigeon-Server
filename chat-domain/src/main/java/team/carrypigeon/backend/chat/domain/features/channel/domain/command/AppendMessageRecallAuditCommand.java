package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

import java.time.Instant;

/**
 * 消息撤回审计追加命令。
 *
 * @param auditLogId 审计记录 ID
 * @param channelId 频道 ID
 * @param actorAccountId 操作账号 ID
 * @param messageId 消息 ID
 * @param senderAccountId 消息发送者账号 ID
 * @param occurredAt 发生时间
 */
public record AppendMessageRecallAuditCommand(
        long auditLogId,
        long channelId,
        long actorAccountId,
        long messageId,
        long senderAccountId,
        Instant occurredAt
) {
}

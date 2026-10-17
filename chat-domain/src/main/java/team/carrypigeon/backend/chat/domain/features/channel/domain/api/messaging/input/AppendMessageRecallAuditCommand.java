package team.carrypigeon.backend.chat.domain.features.channel.domain.api.messaging.input;



import java.time.Instant;

/**
 * 消息撤回审计追加命令。
 * 职责：承载写入一条消息撤回审计事实所需的全部字段。
 * 边界：只承载数据，不承载审计编号生成、权限判断与审计内容渲染。
 *
 * @param auditLogId 审计记录 ID；由调用方生成，本命令不做去重
 * @param channelId 撤回消息所属频道 ID
 * @param actorAccountId 执行撤回操作的账号 ID
 * @param messageId 被撤回消息 ID
 * @param senderAccountId 被撤回消息的发送者账号 ID
 * @param occurredAt 撤回发生时间
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

package team.carrypigeon.backend.chat.domain.features.channel.domain.command;

/**
 * 消息撤回权限校验命令。
 *
 * @param channelId 目标频道 ID
 * @param operatorAccountId 操作账号 ID
 * @param messageChannelId 消息所属频道 ID
 * @param senderAccountId 消息发送者账号 ID
 */
public record RequireMessageRecallPermissionCommand(
        long channelId,
        long operatorAccountId,
        long messageChannelId,
        long senderAccountId
) {
}

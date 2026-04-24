package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 邀请频道成员命令。
 * 职责：承载 private channel 邀请成员用例所需的最小输入。
 * 边界：只表达应用层命令，不承载治理规则判断。
 *
 * @param operatorAccountId 执行邀请的账户 ID
 * @param channelId 频道 ID
 * @param inviteeAccountId 被邀请账户 ID
 */
public record InviteChannelMemberCommand(long operatorAccountId, long channelId, long inviteeAccountId) {
}

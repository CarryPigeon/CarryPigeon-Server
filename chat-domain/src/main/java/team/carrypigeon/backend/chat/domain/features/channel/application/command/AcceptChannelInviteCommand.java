package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 接受频道邀请命令。
 * 职责：承载当前账户接受 private channel 邀请所需的最小输入。
 * 边界：只表达应用层命令，不承载邀请状态转换逻辑。
 *
 * @param accountId 当前账户 ID
 * @param channelId 频道 ID
 */
public record AcceptChannelInviteCommand(long accountId, long channelId) {
}

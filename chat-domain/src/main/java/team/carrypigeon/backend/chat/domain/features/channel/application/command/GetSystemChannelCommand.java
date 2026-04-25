package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * system 频道查询命令。
 * 职责：表达当前认证账户查询 canonical system 频道所需的最小输入。
 *
 * @param accountId 当前账户 ID
 */
public record GetSystemChannelCommand(long accountId) {
}

package team.carrypigeon.backend.chat.domain.features.channel.application.command;

/**
 * 默认频道查询命令。
 * 职责：表达已认证用户查询默认频道的最小输入。
 * 边界：当前阶段仅保留鉴权主体上下文，不扩展筛选条件。
 *
 * @param accountId 当前账户 ID
 */
public record GetDefaultChannelCommand(long accountId) {
}

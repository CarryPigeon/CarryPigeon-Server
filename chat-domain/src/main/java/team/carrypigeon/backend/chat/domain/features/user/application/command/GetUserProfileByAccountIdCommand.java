package team.carrypigeon.backend.chat.domain.features.user.application.command;

/**
 * 按账户 ID 查询用户资料命令。
 * 职责：承载用户资料查询用例的最小输入。
 * 边界：只携带查询标识，不包含任何展示或编辑规则。
 *
 * @param accountId 目标账户 ID
 */
public record GetUserProfileByAccountIdCommand(long accountId) {
}

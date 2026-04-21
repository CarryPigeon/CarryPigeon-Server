package team.carrypigeon.backend.chat.domain.features.user.application.command;

/**
 * 查询当前用户资料命令。
 * 职责：承载当前登录用户资料查询用例需要的最小输入。
 * 边界：这里只表达应用层命令，不承载协议注解与持久化细节。
 *
 * @param accountId 当前登录账户 ID
 */
public record GetCurrentUserProfileCommand(long accountId) {
}

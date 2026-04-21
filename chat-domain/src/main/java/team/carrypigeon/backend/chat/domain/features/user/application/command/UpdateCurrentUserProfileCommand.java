package team.carrypigeon.backend.chat.domain.features.user.application.command;

/**
 * 更新当前用户资料命令。
 * 职责：承载当前登录用户资料更新用例需要的最小输入。
 * 边界：这里只表达应用层命令，不承载协议注解与数据库细节。
 *
 * @param accountId 当前登录账户 ID
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 */
public record UpdateCurrentUserProfileCommand(long accountId, String nickname, String avatarUrl, String bio) {
}

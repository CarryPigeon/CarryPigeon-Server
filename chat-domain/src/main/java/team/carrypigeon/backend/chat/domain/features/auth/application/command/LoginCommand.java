package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 登录命令。
 * 职责：承载用户名密码登录用例需要的最小输入。
 * 边界：这里只表达应用层命令，不包含验证码、令牌或会话语义。
 *
 * @param username 登录用户名
 * @param password 登录密码
 */
public record LoginCommand(String username, String password) {
}

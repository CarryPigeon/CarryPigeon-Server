package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 注册命令。
 * 职责：承载注册用例需要的最小输入。
 * 边界：这里只表达应用层命令，不承载协议注解与持久化细节。
 *
 * @param username 待注册用户名
 * @param password 明文密码输入
 */
public record RegisterCommand(String username, String password) {
}

package team.carrypigeon.backend.chat.domain.features.auth.domain.command;

/**
 * 邮箱验证码签发命令。
 */
public record SendEmailCodeCommand(String email) {
}

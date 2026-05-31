package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 邮箱验证码签发命令。
 */
public record SendEmailCodeCommand(String email) {
}

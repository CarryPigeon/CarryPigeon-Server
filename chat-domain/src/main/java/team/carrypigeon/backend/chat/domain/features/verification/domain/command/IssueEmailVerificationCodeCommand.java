package team.carrypigeon.backend.chat.domain.features.verification.domain.command;

/**
 * 邮箱验证码签发命令。
 *
 * @param email 目标邮箱
 */
public record IssueEmailVerificationCodeCommand(String email) {
}

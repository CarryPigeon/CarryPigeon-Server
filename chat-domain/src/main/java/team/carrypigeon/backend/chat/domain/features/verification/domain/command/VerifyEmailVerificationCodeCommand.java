package team.carrypigeon.backend.chat.domain.features.verification.domain.command;

/**
 * 邮箱验证码校验命令。
 *
 * @param email 目标邮箱
 * @param code 待校验验证码
 */
public record VerifyEmailVerificationCodeCommand(String email, String code) {
}

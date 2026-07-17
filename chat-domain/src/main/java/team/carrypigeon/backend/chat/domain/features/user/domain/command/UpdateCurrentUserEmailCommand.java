package team.carrypigeon.backend.chat.domain.features.user.domain.command;

/**
 * 当前用户邮箱更新命令。
 * 职责：承载更新邮箱所需的账号、新邮箱与验证码。
 * 边界：不暴露 HTTP 请求体或验证码实现细节。
 *
 * @param accountId 当前账号 ID
 * @param email 新邮箱
 * @param code 邮箱验证码
 */
public record UpdateCurrentUserEmailCommand(long accountId, String email, String code) {
}

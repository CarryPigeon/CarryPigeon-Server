package team.carrypigeon.backend.chat.domain.features.auth.domain.command;

/**
 * 当前账号邮箱更新命令。
 *
 * @param accountId 当前账号 ID
 * @param email 新邮箱地址
 * @param code 邮箱验证码
 */
public record UpdateCurrentAccountEmailCommand(long accountId, String email, String code) {
}

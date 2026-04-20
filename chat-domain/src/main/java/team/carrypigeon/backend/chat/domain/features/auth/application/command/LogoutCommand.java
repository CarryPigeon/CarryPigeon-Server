package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 注销命令。
 * 职责：承载 refresh session 撤销用例的最小输入。
 * 边界：当前阶段仅按 refresh token 撤销对应会话。
 *
 * @param refreshToken 待撤销的 refresh token
 */
public record LogoutCommand(String refreshToken) {
}

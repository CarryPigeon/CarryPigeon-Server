package team.carrypigeon.backend.chat.domain.features.auth.application.command;

/**
 * 刷新令牌命令。
 * 职责：承载 refresh token 轮换用例的最小输入。
 * 边界：不包含客户端设备、验证码或权限语义。
 *
 * @param refreshToken 原 refresh token
 */
public record RefreshTokenCommand(String refreshToken) {
}

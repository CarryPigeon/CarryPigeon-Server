package team.carrypigeon.backend.chat.domain.features.auth.application.dto;

/**
 * 登录结果。
 * 职责：向协议层返回登录成功后的最小账户标识信息。
 * 边界：当前任务不返回 JWT、refresh token 或会话状态。
 *
 * @param accountId 登录账户 ID
 * @param username 登录用户名
 */
public record LoginResult(long accountId, String username) {
}

package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

/**
 * 登录响应。
 * 职责：对外返回登录成功后的最小账户标识信息。
 * 边界：当前任务不返回 JWT、refresh token 或其它会话凭证。
 *
 * @param accountId 登录账户 ID
 * @param username 登录用户名
 */
public record LoginResponse(long accountId, String username) {
}

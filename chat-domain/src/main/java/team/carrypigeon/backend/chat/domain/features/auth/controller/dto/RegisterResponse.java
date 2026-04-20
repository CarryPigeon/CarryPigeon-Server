package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

/**
 * 注册响应。
 * 职责：对外返回注册成功后的最小账户标识信息。
 * 边界：不返回密码摘要等敏感字段。
 *
 * @param accountId 新建账户 ID
 * @param username 已注册用户名
 */
public record RegisterResponse(long accountId, String username) {
}

package team.carrypigeon.backend.chat.domain.features.auth.application.dto;

/**
 * 注册结果。
 * 职责：向协议层返回注册成功后的最小稳定数据。
 * 边界：不暴露密码摘要等敏感字段。
 *
 * @param accountId 新建账户 ID
 * @param username 已注册用户名
 */
public record RegisterResult(long accountId, String username) {
}

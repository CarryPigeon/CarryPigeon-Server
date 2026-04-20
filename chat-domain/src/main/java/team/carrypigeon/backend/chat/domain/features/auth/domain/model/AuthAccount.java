package team.carrypigeon.backend.chat.domain.features.auth.domain.model;

import java.time.Instant;

/**
 * 鉴权账户领域模型。
 * 职责：表达当前服务端内最小注册账户语义。
 * 边界：当前阶段仅覆盖注册基础字段，不扩展登录态、角色或刷新令牌语义。
 *
 * @param id 账户 ID
 * @param username 唯一用户名
 * @param passwordHash 已哈希密码摘要
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AuthAccount(
        long id,
        String username,
        String passwordHash,
        Instant createdAt,
        Instant updatedAt
) {
}

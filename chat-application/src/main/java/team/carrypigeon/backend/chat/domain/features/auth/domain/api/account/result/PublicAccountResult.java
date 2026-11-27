package team.carrypigeon.backend.chat.domain.features.auth.domain.api.account.result;

import java.time.Instant;

/**
 * 跨功能账号公开资料；登录邮箱按产品规则公开，密码摘要和维护时间保持在认证内部。
 * @param id 数值账号 ID
 * @param username 公开用户名
 * @param email 任何已认证用户可读的登录邮箱
 * @param createdAt 注册时间
 */
public record PublicAccountResult(long id, String username, String email, Instant createdAt) {}

package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 鉴权账户数据库记录契约。
 * 职责：表达 auth_account 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载注册与登录业务规则。
 *
 * @param id 账户 ID
 * @param username 当前服务端内唯一用户名
 * @param passwordHash 已完成哈希处理的密码摘要
 * @param email 账户邮箱
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AuthAccountRecord(
        long id,
        String username,
        String passwordHash,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
}

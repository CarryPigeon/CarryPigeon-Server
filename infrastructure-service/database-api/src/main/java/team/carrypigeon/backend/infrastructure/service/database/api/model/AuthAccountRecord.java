package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 鉴权账户数据库记录契约。
 * 职责：表达鉴权账户持久化所需的最小统一投影字段。
 * 边界：同时服务查询与写入契约，不承载业务决策语义。
 *
 * @param id 账户主键 ID
 * @param username 当前服务端内唯一用户名
 * @param passwordHash 已完成哈希处理的密码摘要
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AuthAccountRecord(
        long id,
        String username,
        String passwordHash,
        Instant createdAt,
        Instant updatedAt
) {
}

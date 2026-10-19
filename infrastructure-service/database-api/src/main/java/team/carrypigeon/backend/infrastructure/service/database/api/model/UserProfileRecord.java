package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 用户资料数据库记录契约。
 * 职责：表达 user_profile 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载资料校验与展示规则。
 *
 * @param accountId 关联账户 ID
 * @param avatarUrl 用户头像地址
 * @param bio 用户简介
 * @param sex 用户性别协议值
 * @param birthday 用户生日协议值
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserProfileRecord(
        long accountId,
        String avatarUrl,
        String bio,
        long sex,
        long birthday,
        Instant createdAt,
        Instant updatedAt
) {
}

package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 刷新会话数据库记录契约。
 * 职责：表达 auth_refresh_session 表读写共用的最小持久化投影字段。
 * 边界：只服务 database-api 最小数据库契约，不承载 token 轮换与设备治理规则。
 *
 * @param id 刷新会话 ID
 * @param accountId 会话所属账户 ID
 * @param refreshTokenHash refresh token 摘要
 * @param expiresAt 过期时间
 * @param revoked 是否已撤销
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AuthRefreshSessionRecord(
        long id,
        long accountId,
        String refreshTokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt,
        Instant updatedAt
) {
}

package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 刷新会话数据库记录契约。
 * 职责：为 auth refresh session 在 domain 与 database-impl 之间传递最小持久化字段。
 * 边界：不表达设备管理、审计或权限语义。
 *
 * @param id 刷新会话 ID
 * @param accountId 账户 ID
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

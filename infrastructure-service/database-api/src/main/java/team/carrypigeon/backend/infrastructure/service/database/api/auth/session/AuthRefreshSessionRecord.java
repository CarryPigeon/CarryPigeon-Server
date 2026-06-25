package team.carrypigeon.backend.infrastructure.service.database.api.auth.session;

import java.time.Instant;

/**
 * 刷新会话数据库记录契约。
 * 职责：表达刷新会话持久化所需的最小统一投影字段。
 * 边界：同时服务查询、写入与撤销后的读取契约，不表达设备治理语义。
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

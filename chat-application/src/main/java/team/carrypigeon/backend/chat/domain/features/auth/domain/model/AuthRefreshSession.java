package team.carrypigeon.backend.chat.domain.features.auth.domain.model;

import java.time.Instant;

/**
 * 刷新会话领域模型。
 * 职责：表达 refresh token 可被服务端撤销与轮换的最小会话状态。
 * 边界：不承载设备管理、审计日志或多端治理能力。
 *
 * @param id 刷新会话 ID
 * @param accountId 账户 ID
 * @param refreshTokenHash refresh token 摘要
 * @param expiresAt 过期时间
 * @param revoked 是否已撤销
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AuthRefreshSession(
        long id,
        long accountId,
        String refreshTokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt,
        Instant updatedAt
) {
}

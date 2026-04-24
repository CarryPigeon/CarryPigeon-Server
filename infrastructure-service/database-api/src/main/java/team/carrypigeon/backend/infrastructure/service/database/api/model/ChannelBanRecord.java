package team.carrypigeon.backend.infrastructure.service.database.api.model;

import java.time.Instant;

/**
 * 频道封禁数据库记录契约。
 * 职责：在 chat-domain 与 database-impl 之间传递频道封禁字段。
 * 边界：这里只表达数据库服务契约，不承载封禁业务规则。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被封禁账户 ID
 * @param operatorAccountId 执行封禁的账户 ID
 * @param reason 封禁原因
 * @param expiresAt 封禁到期时间；为空表示未设置自动到期
 * @param createdAt 创建时间
 * @param revokedAt 解除封禁时间；为空表示尚未解除
 */
public record ChannelBanRecord(
        long channelId,
        long bannedAccountId,
        long operatorAccountId,
        String reason,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt
) {
}

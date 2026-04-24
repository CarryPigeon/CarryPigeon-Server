package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道封禁领域模型。
 * 职责：表达频道级封禁记录与解除封禁所需的最小持久化状态。
 * 边界：封禁记录独立于活跃成员投影；是否仍然有效由撤销时间与到期时间共同决定。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被封禁账户 ID
 * @param operatorAccountId 执行封禁的账户 ID
 * @param reason 封禁原因
 * @param expiresAt 封禁到期时间；为空表示未设置自动到期
 * @param createdAt 创建时间
 * @param revokedAt 解除封禁时间；为空表示尚未解除
 */
public record ChannelBan(
        long channelId,
        long bannedAccountId,
        long operatorAccountId,
        String reason,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt
) {
}

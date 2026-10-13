package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道禁言领域模型。
 * 职责：表达频道级禁言记录与解除禁言所需的最小持久化状态。
 * 边界：禁言记录独立于活跃成员投影；是否仍然有效由撤销时间与到期时间共同决定。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被禁言账户 ID
 * @param operatorAccountId 执行禁言的账户 ID
 * @param reason 禁言原因
 * @param expiresAt 禁言到期时间；为空表示未设置自动到期
 * @param createdAt 创建时间
 * @param revokedAt 解除禁言时间；为空表示尚未解除
 */
public record ChannelBan(
        long banId,
        long channelId,
        long bannedAccountId,
        long operatorAccountId,
        String reason,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt
) {
}

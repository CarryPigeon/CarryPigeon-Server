package team.carrypigeon.backend.chat.domain.features.channel.domain.model;

import java.time.Instant;

/**
 * 频道禁言领域模型。
 * 职责：表达频道级禁言记录，以及判定该记录是否仍然生效所需的全部状态。
 * 边界：禁言记录独立于活跃成员投影，不随成员关系删除而级联；记录是否生效由 revokedAt 与 expiresAt 共同决定，判定逻辑不在本模型内实现。
 *
 * @param banId 禁言记录主键；对已有记录重新禁言或解除禁言时必须沿用既有值
 * @param channelId 频道 ID
 * @param bannedAccountId 被禁言账户 ID
 * @param operatorAccountId 最近一次执行禁言或解除禁言的账户 ID
 * @param reason 禁言原因；允许空字符串，不允许为 {@code null}
 * @param expiresAt 禁言到期时间；为空表示不设置自动到期，须由解除动作显式结束
 * @param createdAt 禁言记录创建时间
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

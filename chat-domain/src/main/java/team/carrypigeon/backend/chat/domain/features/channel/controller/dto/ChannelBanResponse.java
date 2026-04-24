package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import java.time.Instant;

/**
 * 频道封禁响应。
 * 职责：向 HTTP 协议层返回频道封禁/解封动作后的稳定字段。
 * 边界：只承载协议输出，不承载治理逻辑。
 *
 * @param channelId 频道 ID
 * @param bannedAccountId 被封禁账户 ID
 * @param operatorAccountId 操作人账户 ID
 * @param reason 封禁原因
 * @param expiresAt 到期时间
 * @param createdAt 创建时间
 * @param revokedAt 解封时间
 */
public record ChannelBanResponse(
        long channelId,
        long bannedAccountId,
        long operatorAccountId,
        String reason,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt
) {
}

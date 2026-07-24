package team.carrypigeon.backend.chat.domain.features.channel.domain.projection;

import java.time.Instant;

/**
 * 频道置顶引用投影。
 *
 * @param pinId 置顶记录 ID
 * @param channelId 频道 ID
 * @param messageId 消息 ID
 * @param pinnedByAccountId 操作账号 ID
 * @param note 置顶说明
 * @param pinnedAt 置顶时间
 */
public record ChannelPinReference(
        long pinId,
        long channelId,
        long messageId,
        long pinnedByAccountId,
        String note,
        Instant pinnedAt
) {
}

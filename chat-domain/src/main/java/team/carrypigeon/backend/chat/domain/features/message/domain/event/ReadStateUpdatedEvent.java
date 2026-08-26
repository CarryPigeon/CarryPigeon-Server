package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.time.Instant;

/**
 * 频道消息读位置已更新事件。
 * 职责：表达账号在频道内成功推进后的最后已读消息位置。
 */
public record ReadStateUpdatedEvent(
        long channelId,
        long accountId,
        long lastReadMessageId,
        Instant lastReadTime
) {
}

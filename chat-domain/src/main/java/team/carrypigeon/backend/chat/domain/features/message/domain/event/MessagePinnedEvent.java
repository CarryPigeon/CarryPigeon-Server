package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * 频道消息已置顶事件。
 * 职责：表达新增置顶记录及事务内确定的接收账号快照。
 */
public record MessagePinnedEvent(
        long channelId,
        long messageId,
        long pinId,
        long pinnedByAccountId,
        Instant pinnedAt,
        List<Long> recipientAccountIds
) {

    public MessagePinnedEvent {
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}

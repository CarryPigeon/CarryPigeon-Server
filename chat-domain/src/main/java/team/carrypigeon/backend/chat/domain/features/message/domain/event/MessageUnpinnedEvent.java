package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.util.List;

/**
 * 频道消息置顶已取消事件。
 * 职责：表达被删除的置顶引用、操作者、发生时间与接收账号快照。
 */
public record MessageUnpinnedEvent(
        long channelId,
        long messageId,
        long pinId,
        long unpinnedByAccountId,
        long unpinnedAt,
        List<Long> recipientAccountIds
) {

    public MessageUnpinnedEvent {
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}

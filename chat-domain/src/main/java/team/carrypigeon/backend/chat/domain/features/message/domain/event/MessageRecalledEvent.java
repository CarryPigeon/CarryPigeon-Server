package team.carrypigeon.backend.chat.domain.features.message.domain.event;

import java.util.List;

/**
 * 频道消息已撤回事件。
 * 职责：表达撤回后的消息标识、发生时间与接收账号快照。
 */
public record MessageRecalledEvent(
        long channelId,
        long messageId,
        long recallTime,
        List<Long> recipientAccountIds
) {

    public MessageRecalledEvent {
        recipientAccountIds = List.copyOf(recipientAccountIds);
    }
}

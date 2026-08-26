package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessagePinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageUnpinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.ReadStateUpdatedEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;

/**
 * 消息事实事件的 realtime 映射监听器。
 * 职责：保持既有消息事件名称、payload、接收者和通知偏好标记。
 * 边界：不读取 message 仓储，不参与消息事务或重新计算业务事实。
 */
@Component
public class MessageRealtimeEventListener {

    private final RealtimeEventApi realtimeEventApi;

    public MessageRealtimeEventListener(RealtimeEventApi realtimeEventApi) {
        this.realtimeEventApi = realtimeEventApi;
    }

    @EventListener
    public void onMessageCreated(MessageCreatedEvent event) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("mid", Long.toString(event.messageId()));
        message.put("uid", Long.toString(event.senderId()));
        message.put("cid", Long.toString(event.channelId()));
        message.put("domain", event.domain());
        message.put("domain_version", event.domainVersion());
        message.put("data", event.data());
        message.put("send_time", event.sendTime().toEpochMilli());
        message.put("mentions", event.mentions().stream().map(String::valueOf).toList());
        message.put("preview", event.preview());
        message.put("status", event.status());
        publish(event.channelId(), "message.created", Map.of(
                "cid", Long.toString(event.channelId()), "message", message
        ), event.recipientAccountIds());
    }

    @EventListener
    public void onMessageRecalled(MessageRecalledEvent event) {
        publish(event.channelId(), "message.recalled", Map.of(
                "cid", Long.toString(event.channelId()),
                "mid", Long.toString(event.messageId()),
                "recall_time", event.recallTime()
        ), event.recipientAccountIds());
    }

    @EventListener
    public void onMessagePinned(MessagePinnedEvent event) {
        publish(event.channelId(), "message.pinned", Map.of(
                "cid", Long.toString(event.channelId()),
                "mid", Long.toString(event.messageId()),
                "pin_id", Long.toString(event.pinId()),
                "pinned_by_uid", Long.toString(event.pinnedByAccountId()),
                "pinned_at", event.pinnedAt().toEpochMilli()
        ), event.recipientAccountIds());
    }

    @EventListener
    public void onMessageUnpinned(MessageUnpinnedEvent event) {
        publish(event.channelId(), "message.unpinned", Map.of(
                "cid", Long.toString(event.channelId()),
                "mid", Long.toString(event.messageId()),
                "pin_id", Long.toString(event.pinId()),
                "unpinned_by_uid", Long.toString(event.unpinnedByAccountId()),
                "unpinned_at", event.unpinnedAt()
        ), event.recipientAccountIds());
    }

    @EventListener
    public void onMentionCreated(MentionCreatedEvent event) {
        publish(event.channelId(), "mention.created", Map.of(
                "mention_id", Long.toString(event.mentionId()),
                "cid", Long.toString(event.channelId()),
                "mid", Long.toString(event.messageId()),
                "from_uid", Long.toString(event.fromAccountId()),
                "uid", Long.toString(event.targetAccountId()),
                "created_at", event.createdAt().toEpochMilli()
        ), List.of(event.targetAccountId()));
    }

    @EventListener
    public void onReadStateUpdated(ReadStateUpdatedEvent event) {
        publish(event.channelId(), "read_state.updated", Map.of(
                "cid", Long.toString(event.channelId()),
                "uid", Long.toString(event.accountId()),
                "last_read_mid", Long.toString(event.lastReadMessageId()),
                "last_read_time", event.lastReadTime().toEpochMilli()
        ), List.of(event.accountId()));
    }

    private void publish(long channelId, String eventType, Object payload, List<Long> recipients) {
        realtimeEventApi.publish(new PublishRealtimeEventCommand(channelId, eventType, payload, recipients, true));
    }
}

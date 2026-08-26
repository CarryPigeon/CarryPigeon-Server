package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.AccountChannelsChangedEvent;
import team.carrypigeon.backend.chat.domain.features.channel.domain.event.ChannelChangedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessagePinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageUnpinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.ReadStateUpdatedEvent;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 领域事实事件到 realtime 命令的映射契约测试。
 * 职责：锁定事件名称、payload、接收者和通知偏好过滤标记，防止协议在依赖反转时漂移。
 */
@Tag("contract")
class RealtimeDomainEventListenerTests {

    private static final Instant BASE_TIME = Instant.parse("2026-04-24T12:00:00Z");

    /**
     * 验证两类频道事实保持既有 realtime 路由和通知偏好策略。
     */
    @Test
    @DisplayName("channel facts map stable realtime commands")
    void channelFacts_validEvents_mapStableRealtimeCommands() {
        RecordingRealtimeEventApi realtime = new RecordingRealtimeEventApi();
        ChannelRealtimeEventListener listener = new ChannelRealtimeEventListener(realtime);

        listener.onChannelChanged(new ChannelChangedEvent(9L, "members", List.of(1001L, 1002L)));
        listener.onAccountChannelsChanged(new AccountChannelsChangedEvent(1002L));

        PublishRealtimeEventCommand channelChanged = realtime.commands.get(0);
        assertEquals("channel.changed", channelChanged.eventType());
        assertEquals(9L, channelChanged.channelId());
        assertEquals("members", payload(channelChanged).get("scope"));
        assertEquals(List.of(1001L, 1002L), channelChanged.recipientAccountIds());
        assertTrue(channelChanged.applyNotificationPreferences());
        PublishRealtimeEventCommand channelsChanged = realtime.commands.get(1);
        assertEquals("channels.changed", channelsChanged.eventType());
        assertNull(channelsChanged.channelId());
        assertEquals(List.of(1002L), channelsChanged.recipientAccountIds());
        assertFalse(channelsChanged.applyNotificationPreferences());
    }

    /**
     * 验证 canonical 消息事实映射为原有嵌套 message payload。
     */
    @Test
    @DisplayName("message created fact maps canonical payload")
    void onMessageCreated_validEvent_mapsCanonicalPayload() {
        RecordingRealtimeEventApi realtime = new RecordingRealtimeEventApi();
        MessageRealtimeEventListener listener = new MessageRealtimeEventListener(realtime);
        listener.onMessageCreated(new MessageCreatedEvent(
                5001L, 1001L, 9L, "Core:Forward", "1.0.0",
                Map.of("forwarded_from", Map.of("mid", "5000")), BASE_TIME,
                List.of(1002L), "forward", "sent", List.of(1001L, 1002L)
        ));

        PublishRealtimeEventCommand command = realtime.commands.getFirst();
        Map<String, Object> message = map(payload(command).get("message"));
        assertEquals("message.created", command.eventType());
        assertEquals("5001", message.get("mid"));
        assertEquals("Core:Forward", message.get("domain"));
        assertEquals(List.of("1002"), message.get("mentions"));
        assertEquals(Map.of("mid", "5000"), map(message.get("data")).get("forwarded_from"));
        assertEquals(List.of(1001L, 1002L), command.recipientAccountIds());
        assertTrue(command.applyNotificationPreferences());
    }

    /**
     * 验证撤回、置顶和取消置顶事实保留既有事件名与关键时间字段。
     */
    @Test
    @DisplayName("message lifecycle facts map stable payloads")
    void messageLifecycleFacts_validEvents_mapStablePayloads() {
        RecordingRealtimeEventApi realtime = new RecordingRealtimeEventApi();
        MessageRealtimeEventListener listener = new MessageRealtimeEventListener(realtime);

        listener.onMessageRecalled(new MessageRecalledEvent(9L, 5001L, BASE_TIME.toEpochMilli(), List.of(1001L)));
        listener.onMessagePinned(new MessagePinnedEvent(9L, 5001L, 7001L, 1001L, BASE_TIME, List.of(1001L)));
        listener.onMessageUnpinned(new MessageUnpinnedEvent(9L, 5001L, 7001L, 1002L,
                BASE_TIME.toEpochMilli(), List.of(1001L, 1002L)));

        assertEquals("message.recalled", realtime.commands.get(0).eventType());
        assertEquals(BASE_TIME.toEpochMilli(), payload(realtime.commands.get(0)).get("recall_time"));
        assertEquals("message.pinned", realtime.commands.get(1).eventType());
        assertEquals("7001", payload(realtime.commands.get(1)).get("pin_id"));
        assertEquals("message.unpinned", realtime.commands.get(2).eventType());
        assertEquals("1002", payload(realtime.commands.get(2)).get("unpinned_by_uid"));
    }

    /**
     * 验证 mention 与读状态事实只投递给对应账号，并保留原 payload 字段。
     */
    @Test
    @DisplayName("account message facts target corresponding account")
    void accountMessageFacts_validEvents_targetCorrespondingAccount() {
        RecordingRealtimeEventApi realtime = new RecordingRealtimeEventApi();
        MessageRealtimeEventListener listener = new MessageRealtimeEventListener(realtime);

        listener.onMentionCreated(new MentionCreatedEvent(8001L, 9L, 5001L, 1001L, 1002L, BASE_TIME));
        listener.onReadStateUpdated(new ReadStateUpdatedEvent(9L, 1001L, 5001L, BASE_TIME));

        assertEquals("mention.created", realtime.commands.get(0).eventType());
        assertEquals(List.of(1002L), realtime.commands.get(0).recipientAccountIds());
        assertEquals("1002", payload(realtime.commands.get(0)).get("uid"));
        assertEquals("read_state.updated", realtime.commands.get(1).eventType());
        assertEquals(List.of(1001L), realtime.commands.get(1).recipientAccountIds());
        assertEquals("5001", payload(realtime.commands.get(1)).get("last_read_mid"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(PublishRealtimeEventCommand command) {
        return (Map<String, Object>) command.payload();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class RecordingRealtimeEventApi implements RealtimeEventApi {

        private final List<PublishRealtimeEventCommand> commands = new ArrayList<>();

        @Override
        public void publish(PublishRealtimeEventCommand command) {
            commands.add(command);
        }
    }
}

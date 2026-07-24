package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeServerMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeNotificationPreferenceFilter;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时事件 API 实现。
 * 职责：应用通知偏好、写入事件缓存并向目标账号的在线会话投递 v1 event frame。
 * 边界：只理解通用事件命令，不引用 message 或 channel 内部模型。
 */
@Service
public class RealtimeEventDomainApi implements RealtimeEventApi {

    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final RealtimeNotificationPreferenceFilter notificationPreferenceFilter;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;
    private final IdGenerator idGenerator;

    public RealtimeEventDomainApi(
            RealtimeSessionRegistry realtimeSessionRegistry,
            RealtimeNotificationPreferenceFilter notificationPreferenceFilter,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.notificationPreferenceFilter = notificationPreferenceFilter;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.idGenerator = idGenerator;
    }

    @Override
    public void publish(PublishRealtimeEventCommand command) {
        Collection<Long> recipients = command.applyNotificationPreferences()
                ? notificationPreferenceFilter.filterRecipients(
                        command.channelId(),
                        command.eventType(),
                        command.recipientAccountIds()
                )
                : command.recipientAccountIds();
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        String eventId = idGenerator.nextStringId();
        long serverTime = timeProvider.nowMillis();
        realtimeSessionRegistry.appendEvent(RealtimeSessionRegistry.event(
                eventId,
                command.eventType(),
                serverTime,
                command.payload(),
                recipients
        ));
        String frameText = jsonProvider.toJson(new RealtimeServerMessage(
                "event",
                null,
                Map.of(
                        "event_id", eventId,
                        "event_type", command.eventType(),
                        "server_time", serverTime,
                        "payload", command.payload()
                ),
                null
        ));
        for (Long recipient : recipients) {
            realtimeSessionRegistry.getChannels(recipient)
                    .forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(frameText)));
        }
    }
}

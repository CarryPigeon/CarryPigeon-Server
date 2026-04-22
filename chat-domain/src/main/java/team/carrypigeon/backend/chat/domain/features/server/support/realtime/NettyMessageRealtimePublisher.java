package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import org.springframework.context.annotation.Primary;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeServerMessage;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 基于 Netty 的消息实时发布器。
 * 职责：把已持久化业务消息分发给在线成员通道。
 * 边界：只负责实时投递，不参与消息鉴权与持久化规则。
 */
@Primary
public class NettyMessageRealtimePublisher implements MessageRealtimePublisher {

    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;

    public NettyMessageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
    }

    @Override
    public void publish(ChannelMessage message, Collection<Long> recipientAccountIds) {
        String frameText = jsonProvider.toJson(new RealtimeServerMessage(
                "channel_message",
                null,
                timeProvider.nowMillis(),
                new RealtimeChannelMessagePayload(
                        message.messageId(),
                        message.serverId(),
                        message.conversationId(),
                        message.channelId(),
                        message.senderId(),
                        message.messageType(),
                        message.body(),
                        message.previewText(),
                        message.payload(),
                        message.metadata(),
                        message.status(),
                        message.createdAt()
                )
        ));
        for (Long recipientAccountId : recipientAccountIds) {
            realtimeSessionRegistry.getChannels(recipientAccountId)
                    .forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(frameText)));
        }
    }

    private record RealtimeChannelMessagePayload(
            long messageId,
            String serverId,
            long conversationId,
            long channelId,
            long senderId,
            String messageType,
            String body,
            String previewText,
            String payload,
            String metadata,
            String status,
            java.time.Instant createdAt
    ) {
    }
}

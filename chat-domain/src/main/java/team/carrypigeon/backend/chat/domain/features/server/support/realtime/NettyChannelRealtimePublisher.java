package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeServerMessage;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 基于 Netty 的频道实时发布器。
 * 职责：把频道视图与读状态变更转换为 v1 `event` envelope 并分发给目标会话。
 * 边界：只负责实时投递与最小事件日志记录，不参与频道鉴权与持久化规则。
 */
public class NettyChannelRealtimePublisher implements ChannelRealtimePublisher {

    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;
    private final IdGenerator idGenerator;

    public NettyChannelRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator
    ) {
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.idGenerator = idGenerator;
    }

    /**
     * 广播单个账户的已读状态更新。
     * 副作用：写入实时事件缓存并只向目标账户在线会话推送事件。
     */
    @Override
    public void publishReadStateUpdated(ChannelReadState readState) {
        publishEvent("read_state.updated", Map.of(
                "cid", Long.toString(readState.channelId()),
                "uid", Long.toString(readState.accountId()),
                "last_read_mid", Long.toString(readState.lastReadMessageId()),
                "last_read_time", readState.lastReadTime().toEpochMilli()
        ), List.of(readState.accountId()));
    }

    /**
     * 广播频道局部变化提示。
     * 输入：频道、变化范围和接收者集合。
     * 约束：事件只给出 `refresh` hint，不透传完整频道快照。
     */
    @Override
    public void publishChannelChanged(Channel channel, String scope, Collection<Long> recipientAccountIds) {
        publishEvent("channel.changed", Map.of(
                "cid", Long.toString(channel.id()),
                "scope", scope == null || scope.isBlank() ? "profile" : scope,
                "hint", "refresh"
        ), recipientAccountIds);
    }

    /**
     * 广播账户可见频道集合已变化。
     */
    @Override
    public void publishChannelsChanged(long accountId) {
        publishEvent("channels.changed", Map.of("hint", "refresh"), List.of(accountId));
    }

    private void publishEvent(String eventType, Object payload, Collection<Long> recipientAccountIds) {
        String eventId = idGenerator.nextStringId();
        long serverTime = timeProvider.nowMillis();
        realtimeSessionRegistry.appendEvent(RealtimeSessionRegistry.event(
                eventId,
                eventType,
                serverTime,
                payload,
                recipientAccountIds
        ));
        String frameText = jsonProvider.toJson(new RealtimeServerMessage(
                "event",
                null,
                Map.of(
                        "event_id", eventId,
                        "event_type", eventType,
                        "server_time", serverTime,
                        "payload", payload
                ),
                null
        ));
        for (Long recipientAccountId : recipientAccountIds) {
            realtimeSessionRegistry.getChannels(recipientAccountId)
                    .forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(frameText)));
        }
    }
}

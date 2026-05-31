package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelReadState;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NettyChannelRealtimePublisher 契约测试。
 * 职责：验证频道读状态与频道视图刷新提示会转换为 docs/t 定义的 realtime 事件。
 * 边界：不验证完整 Netty 握手与鉴权链，只验证发布器出站载荷语义。
 */
@Tag("contract")
class NettyChannelRealtimePublisherTests {

    @Test
    @DisplayName("publish read state updated emits read state updated event")
    void publishReadStateUpdated_emitsReadStateUpdatedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyChannelRealtimePublisher publisher = new NettyChannelRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:10Z"), ZoneOffset.UTC)),
                () -> 9001L
        );

        publisher.publishReadStateUpdated(new ChannelReadState(9L, 1001L, 5001L, Instant.parse("2026-04-24T12:00:05Z"), Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:05Z")));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("read_state.updated", root.path("data").path("event_type").asText());
        assertEquals("9", root.path("data").path("payload").path("cid").asText());
        assertEquals("5001", root.path("data").path("payload").path("last_read_mid").asText());
    }

    @Test
    @DisplayName("publish channel changed emits channel changed event")
    void publishChannelChanged_emitsChannelChangedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1001L, channel);
        NettyChannelRealtimePublisher publisher = new NettyChannelRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:10Z"), ZoneOffset.UTC)),
                () -> 9002L
        );

        publisher.publishChannelChanged(new Channel(9L, 9L, "general", "", "", "", "private", false, Instant.parse("2026-04-24T12:00:00Z"), Instant.parse("2026-04-24T12:00:00Z")), "members", List.of(1001L));

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("channel.changed", root.path("data").path("event_type").asText());
        assertEquals("9", root.path("data").path("payload").path("cid").asText());
        assertEquals("members", root.path("data").path("payload").path("scope").asText());
        assertEquals("refresh", root.path("data").path("payload").path("hint").asText());
    }

    @Test
    @DisplayName("publish channels changed emits channels changed event")
    void publishChannelsChanged_emitsChannelsChangedEvent() {
        JsonProvider jsonProvider = jsonProvider();
        RealtimeSessionRegistry realtimeSessionRegistry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        realtimeSessionRegistry.register(1002L, channel);
        NettyChannelRealtimePublisher publisher = new NettyChannelRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-24T12:00:10Z"), ZoneOffset.UTC)),
                () -> 9003L
        );

        publisher.publishChannelsChanged(1002L);

        TextWebSocketFrame frame = channel.readOutbound();

        assertNotNull(frame);
        var root = jsonProvider.readTree(frame.text());
        assertEquals("channels.changed", root.path("data").path("event_type").asText());
        assertEquals("refresh", root.path("data").path("payload").path("hint").asText());
    }

    private static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }
}

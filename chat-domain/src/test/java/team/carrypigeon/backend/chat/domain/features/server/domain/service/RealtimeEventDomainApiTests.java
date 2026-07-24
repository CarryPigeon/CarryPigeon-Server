package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeNotificationPreferenceFilter;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RealtimeEventDomainApi 契约测试。
 * 职责：验证通用事件会写入缓存并向目标账号在线会话发送 v1 event frame。
 */
@Tag("contract")
class RealtimeEventDomainApiTests {

    /**
     * 验证在线接收人会获得稳定 v1 event envelope，且事件同时进入断线续传缓存。
     */
    @Test
    @DisplayName("publish online recipient stores event and delivers stable v1 envelope")
    void publish_onlineRecipient_storesAndDeliversEvent() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        registry.register(1001L, channel);
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper().findAndRegisterModules());
        RealtimeEventDomainApi api = new RealtimeEventDomainApi(
                registry,
                RealtimeNotificationPreferenceFilter.allowAll(),
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new IdGenerator() {
                    @Override public long nextLongId() { return 9001L; }
                }
        );

        api.publish(new PublishRealtimeEventCommand(
                1L, "channel.changed", Map.of("hint", "refresh"), List.of(1001L), true));

        TextWebSocketFrame frame = channel.readOutbound();
        assertNotNull(frame);
        JsonNode root = jsonProvider.readTree(frame.text());
        assertEquals("event", root.path("type").asText());
        assertEquals("9001", root.path("data").path("event_id").asText());
        assertEquals("channel.changed", root.path("data").path("event_type").asText());
        assertEquals(1776816000000L, root.path("data").path("server_time").asLong());
        assertEquals("refresh", root.path("data").path("payload").path("hint").asText());
        assertEquals(List.of(), registry.eventsAfter(1001L, "9001"));
        frame.release();
    }
}

package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 消息分发契约测试。
 * 职责：验证 ping、resume.failed 与不受支持命令的稳定错误行为。
 * 边界：不验证完整实时事件矩阵，只验证 round5 关键命令流。
 */
@Tag("contract")
class RealtimeChannelHandlerMessageDispatchTests {

    /**
     * 验证 `channelRead` 在 `ping` 条件下满足 `returnsPong` 的测试契约。
     */
    @Test
    @DisplayName("ping command returns pong frame")
    void channelRead_ping_returnsPong() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"ping"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"pong\""));
    }

    /**
     * 验证 `channelRead` 在 `authWithStaleResume` 条件下满足 `returnsResumeFailed` 的测试契约。
     */
    @Test
    @DisplayName("auth with stale resume returns resume failed")
    void channelRead_authWithStaleResume_returnsResumeFailed() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1","resume":{"last_event_id":"too-old"}}}
                """));

        sender.readOutbound();
        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"resume.failed\""));
        assertTrue(frame.text().contains("event_too_old"));
    }

    /**
     * 验证 `channelRead` 在 `authResume` 条件下满足 `onlyReplaysEventsForCurrentAccount` 的测试契约。
     */
    @Test
    @DisplayName("auth resume only replays events for current account")
    void channelRead_authResume_onlyReplaysEventsForCurrentAccount() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        registry.appendEvent(RealtimeSessionRegistry.event("event-1", "mention.created", 1L, java.util.Map.of("mid", "1"), java.util.List.of(1002L)));
        registry.appendEvent(RealtimeSessionRegistry.event("event-2", "message.created", 2L, java.util.Map.of("mid", "2"), java.util.List.of(1001L)));
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token-2","device_id":"device-1","resume":{"last_event_id":"event-1"}}}
                """));

        TextWebSocketFrame authOk = sender.readOutbound();
        assertNotNull(authOk);
        assertTrue(authOk.text().contains("\"type\":\"auth.ok\""));
        assertNull(sender.readOutbound());
    }

    /**
     * 验证 auth token subject 不是账号 ID 时返回 auth.err unauthorized，而不是 internal_error。
     */
    @Test
    @DisplayName("auth non numeric subject returns unauthorized error")
    void channelRead_authNonNumericSubject_returnsUnauthorizedError() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"bad-subject-token","device_id":"device-1"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"auth.err\""), frame.text());
        assertTrue(frame.text().contains("\"reason\":\"unauthorized\""), frame.text());
        assertTrue(frame.text().contains("access token is invalid"), frame.text());
    }

    /**
     * 验证 `channelRead` 在 `unsupportedCommandAfterAuth` 条件下满足 `returnsValidationError` 的测试契约。
     */
    @Test
    @DisplayName("unsupported command after auth returns validation error")
    void channelRead_unsupportedCommandAfterAuth_returnsValidationError() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1"}}
                """));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"unsupported_command","data":{"channel_id":1,"message_type":"text","body":"hello world"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"unsupported_command.err\""));
        assertTrue(frame.text().contains("\"reason\":\"validation_failed\""));
    }

    /**
     * 验证 `channelRead` 在 `sendChannelMessageAfterAuth` 条件下满足 `publishesChannelMessageFrame` 的测试契约。
     */
    @Test
    @DisplayName("send channel message after auth publishes channel message frame")
    void channelRead_sendChannelMessageAfterAuth_publishesChannelMessageFrame() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1"}}
                """));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","id":"2","data":{"channel_id":1,"message_type":"text","body":"hello world"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"event\""), frame.text());
        assertTrue(frame.text().contains("\"event_type\":\"message.created\""), frame.text());
        assertTrue(frame.text().contains("hello world"), frame.text());
    }

    /**
     * 验证 malformed 入站命令会返回可修正的校验错误，而不是 internal_error。
     */
    @Test
    @DisplayName("send channel message malformed numeric field returns validation error")
    void channelRead_sendChannelMessageMalformedNumericField_returnsValidationError() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1"}}
                """));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","id":"2","data":{"channel_id":"abc","message_type":"text","body":"hello world"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"send_channel_message.err\""), frame.text());
        assertTrue(frame.text().contains("\"reason\":\"validation_failed\""), frame.text());
        assertTrue(frame.text().contains("channel_id must be decimal number"), frame.text());
    }
}

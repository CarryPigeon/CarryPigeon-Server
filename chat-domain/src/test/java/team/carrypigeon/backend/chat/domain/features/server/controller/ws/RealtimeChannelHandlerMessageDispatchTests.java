package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 消息分发契约测试。
 * 职责：验证 ping、resume.failed 与不受支持命令的稳定错误行为。
 * 边界：不验证完整实时事件矩阵，只验证 round5 关键命令流。
 */
@Tag("contract")
class RealtimeChannelHandlerMessageDispatchTests {

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
}

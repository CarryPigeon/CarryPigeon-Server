package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 生命周期契约测试。
 * 职责：验证握手后不会发送旧 welcome，且首帧 auth 可以建立会话。
 * 边界：不验证业务消息分发，只验证生命周期与首帧鉴权。
 */
@Tag("contract")
class RealtimeChannelHandlerLifecycleTests {

    @Test
    @DisplayName("handshake complete does not send welcome before auth")
    void userEvent_handshakeComplete_doesNotSendWelcomeBeforeAuth() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));

        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        assertNull(sender.readOutbound());
    }

    @Test
    @DisplayName("auth frame registers principal and replies auth ok")
    void channelRead_authFrame_registersPrincipalAndRepliesAuthOk() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"type\":\"auth.ok\""));
        assertTrue(frame.text().contains("\"uid\":\"1001\""));
        assertEquals(1, registry.getChannels(1001L).size());
    }
}

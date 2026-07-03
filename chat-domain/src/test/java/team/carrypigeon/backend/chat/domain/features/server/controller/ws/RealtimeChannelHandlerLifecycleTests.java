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

    /**
     * 验证 `userEvent` 在 `handshakeComplete` 条件下满足 `doesNotSendWelcomeBeforeAuth` 的测试契约。
     */
    @Test
    @DisplayName("handshake complete does not send welcome before auth")
    void userEvent_handshakeComplete_doesNotSendWelcomeBeforeAuth() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));

        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        assertNull(sender.readOutbound());
    }

    /**
     * 验证 `channelRead` 在 `authFrame` 条件下满足 `registersPrincipalAndRepliesAuthOk` 的测试契约。
     */
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

    /**
     * 验证 `channelRead` 在 `reauthFrame` 条件下满足 `movesChannelRegistrationToNewAccount` 的测试契约。
     */
    @Test
    @DisplayName("reauth frame moves channel registration to new account")
    void channelRead_reauthFrame_movesChannelRegistrationToNewAccount() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","device_id":"device-1"}}
                """));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"reauth","id":"2","data":{"access_token":"access-token-2","device_id":"device-1"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"type\":\"reauth.ok\""));
        assertEquals(0, registry.getChannels(1001L).size());
        assertEquals(1, registry.getChannels(1002L).size());
    }
}

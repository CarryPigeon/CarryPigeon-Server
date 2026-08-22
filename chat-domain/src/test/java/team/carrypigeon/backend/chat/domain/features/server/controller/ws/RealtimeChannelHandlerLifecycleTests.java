package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry);

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
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry);
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
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry);
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

    /**
     * 验证未鉴权连接不能通过 ping 续命。
     * 输入：握手后未发 auth 直接发送 ping。
     * 输出：服务端返回 unauthorized auth.err 并关闭通道。
     */
    @Test
    @DisplayName("unauthenticated ping replies unauthorized and closes channel")
    void channelRead_unauthenticatedPing_repliesUnauthorizedAndClosesChannel() {
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(new RealtimeSessionRegistry());
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"ping","id":"ping-1"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"type\":\"auth.err\""));
        assertTrue(frame.text().contains("\"reason\":\"unauthorized\""));
        assertFalse(sender.isActive());
    }

    /**
     * 验证已鉴权连接可通过 ping/pong 完成心跳检测。
     * 输入：先发 auth，再发 ping。
     * 输出：服务端返回 pong，且通道保持活跃。
     */
    @Test
    @DisplayName("authenticated ping replies pong and keeps channel active")
    void channelRead_authenticatedPing_repliesPongAndKeepsChannelActive() {
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(new RealtimeSessionRegistry());
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token"}}
                """));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"ping","id":"ping-1"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"type\":\"pong\""));
        assertTrue(sender.isActive());
    }

    /**
     * 验证 access token 到期后实时连接会主动下发 token_expired 并关闭。
     */
    @Test
    @DisplayName("access token expiration replies error and closes channel")
    void userEvent_accessTokenExpiration_repliesErrorAndClosesChannel() {
        Instant expiresAt = Instant.parse("2026-04-22T00:00:00Z").plusMillis(100);
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(
                new RealtimeSessionRegistry(),
                10,
                ignored -> new AccessTokenAuthenticationResult(1001L, "carry-user", expiresAt)
        );
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token"}}
                """));
        sender.readOutbound();

        sender.advanceTimeBy(1, TimeUnit.SECONDS);
        sender.runScheduledPendingTasks();

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"reason\":\"token_expired\""));
        assertFalse(sender.isActive());
    }

    /**
     * 验证握手后长时间未发首帧 auth 的连接会被主动清理。
     * 输入：一秒鉴权时限且不发送任何帧。
     * 输出：超时后返回 authentication_timeout 并关闭通道。
     */
    @Test
    @DisplayName("authentication timeout replies error and closes channel")
    void userEvent_authenticationTimeout_repliesErrorAndClosesChannel() {
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(new RealtimeSessionRegistry(), 1);
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));

        sender.advanceTimeBy(1, TimeUnit.SECONDS);
        sender.runScheduledPendingTasks();

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"type\":\"auth.err\""));
        assertTrue(frame.text().contains("\"reason\":\"authentication_timeout\""));
        assertFalse(sender.isActive());
    }

    /**
     * 验证读空闲事件不会再被吞掉。
     * 输入：已鉴权连接上的 reader idle 事件。
     * 输出：服务端返回 idle_timeout 并关闭通道。
     */
    @Test
    @DisplayName("reader idle event replies error and closes channel")
    void userEvent_readerIdle_repliesErrorAndClosesChannel() {
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(new RealtimeSessionRegistry());
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null));
        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token"}}
                """));
        sender.readOutbound();

        sender.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);

        TextWebSocketFrame frame = sender.readOutbound();
        assertTrue(frame.text().contains("\"reason\":\"idle_timeout\""));
        assertFalse(sender.isActive());
    }
}

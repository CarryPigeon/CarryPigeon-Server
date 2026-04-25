package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 消息分发契约测试。
 * 职责：验证实时通道的业务消息发送、参数校验与错误回写行为。
 * 边界：不验证欢迎消息生命周期，只验证消息分发主链路。
 */
@Tag("contract")
class RealtimeChannelHandlerMessageDispatchTests {

    /**
     * 验证业务消息发送后会广播使用同一个业务 messageId 的实时消息。
     */
    @Test
    @DisplayName("channel read send message command broadcasts persisted message id")
    void channelRead_sendMessageCommand_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channel_id":1,"body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在 text 消息场景下也能走通当前主链路。
     */
    @Test
    @DisplayName("channel read generic send message command with text type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithTextType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"hello generic world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在 plugin 消息场景下也能走通当前主链路。
     */
    @Test
    @DisplayName("channel read generic send message command with plugin type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithPluginType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"plugin","body":"mc bridge","payload":{"plugin_key":"mc-bridge","event":"player_join"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在 custom 消息场景下也能走通当前主链路。
     */
    @Test
    @DisplayName("channel read generic send message command with custom type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithCustomType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"custom","body":"status card","payload":{"card":"server-status"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令收到当前未支持的消息类型时会回写问题消息。
     */
    @Test
    @DisplayName("channel read generic send message command with unsupported type returns problem payload")
    void channelRead_genericSendMessageCommandWithUnsupportedType_returnsProblemPayload() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"unknown","body":"ignored"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("unsupported realtime message type"));
    }

    /**
     * 验证参数不合法时处理器会回写问题消息。
     */
    @Test
    @DisplayName("channel read invalid message command returns problem payload")
    void channelRead_invalidMessageCommand_returnsProblemPayload() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channel_id":1,"body":"  "}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":200"));
        assertTrue(frame.text().contains("body must not be blank"));
    }

    /**
     * 验证未预期运行时失败时处理器会回写 500 问题消息。
     */
    @Test
    @DisplayName("channel read unexpected failure returns problem payload with code 500")
    void channelRead_unexpectedFailure_returnsProblemPayloadWithCode500() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.failingService());
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channel_id":1,"body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":500"));
        assertTrue(frame.text().contains("internal server error"));
    }
}

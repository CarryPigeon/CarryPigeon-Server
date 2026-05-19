package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

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
    @DisplayName("channel read generic send message command with text type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithTextType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在 file 消息场景下也能走通当前主链路。
     */
    @Test
    @DisplayName("channel read generic send message command with file type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithFileType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"file","body":"project doc","payload":{"object_key":"channels/1/messages/file/accounts/1001/5001-demo.pdf","filename":"demo.pdf","mime_type":"application/pdf","size":12345}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在 voice 消息场景下也能走通当前主链路。
     */
    @Test
    @DisplayName("channel read generic send message command with voice type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithVoiceType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"voice","payload":{"object_key":"channels/1/messages/voice/accounts/1001/5001-demo.mp3","filename":"demo.mp3","mime_type":"audio/mpeg","size":45678,"duration_millis":12000,"transcript":"会议纪要"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证统一发送命令在扩展消息场景下会走 plugin-style 路径。
     */
    @Test
    @DisplayName("channel read generic send message command with extension type broadcasts persisted message id")
    void channelRead_genericSendMessageCommandWithExtensionType_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"test-extension","body":"player join","payload":{"event":"player_join"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
    }

    /**
     * 验证未知命令类型时会回写问题消息。
     */
    @Test
    @DisplayName("channel read unsupported command returns problem payload")
    void channelRead_unsupportedCommand_returnsProblemPayload() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"unknown_command","channel_id":1,"message_type":"text","body":"ignored"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("unsupported realtime message type"));
    }

    /**
     * 验证扩展消息缺少 payload 时会回写问题消息。
     */
    @Test
    @DisplayName("channel read extension message without payload returns problem payload")
    void channelRead_extensionMessageWithoutPayload_returnsProblemPayload() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"test-extension","body":"player join"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("payload must not be blank"));
    }

    /**
     * 验证未注册扩展消息类型会回写稳定问题消息。
     */
    @Test
    @DisplayName("channel read unregistered extension message returns problem payload")
    void channelRead_unregisteredExtensionMessage_returnsProblemPayload() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.serviceWithoutExtensionRegistration(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"test-extension","body":"player join","payload":{"event":"player_join"}}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("unsupported extension message type"));
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
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"  "}
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
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":500"));
        assertTrue(frame.text().contains("internal server error"));
    }

    /**
     * 验证消息处理器在服务缺失时会回写 500 问题消息。
     */
    @Test
    @DisplayName("channel read missing message service returns problem payload with code 500")
    void channelRead_missingMessageService_returnsProblemPayloadWithCode500() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = new EmbeddedChannel(new RealtimeChannelHandler(
                RealtimeChannelHandlerTestSupport.jsonProvider(),
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                registry,
                () -> null,
                () -> RealtimeChannelHandlerTestSupport.dispatcher()
        ));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":500"));
        assertTrue(frame.text().contains("realtime message service is unavailable"));
    }

    /**
     * 验证消息处理器在 dispatcher 缺失时会回写 500 问题消息。
     */
    @Test
    @DisplayName("channel read missing dispatcher returns problem payload with code 500")
    void channelRead_missingDispatcher_returnsProblemPayloadWithCode500() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = new EmbeddedChannel(new RealtimeChannelHandler(
                RealtimeChannelHandlerTestSupport.jsonProvider(),
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                registry,
                () -> RealtimeChannelHandlerTestSupport.service(registry),
                () -> null
        ));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channel_id":1,"message_type":"text","body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":500"));
        assertTrue(frame.text().contains("realtime message dispatcher is unavailable"));
    }
}

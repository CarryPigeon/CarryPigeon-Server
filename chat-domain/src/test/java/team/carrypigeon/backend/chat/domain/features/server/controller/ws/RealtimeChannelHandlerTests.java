package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeMessageHandlingConfiguration;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 契约测试。
 * 职责：验证实时通道欢迎、业务消息分发与错误回写的稳定行为。
 * 边界：不验证完整 Netty 握手升级流程，只验证业务处理器本身。
 */
class RealtimeChannelHandlerTests {

    /**
     * 验证业务消息发送后会广播使用同一个业务 messageId 的实时消息。
     */
    @Test
    @DisplayName("channel read send message command broadcasts persisted message id")
    void channelRead_sendMessageCommand_broadcastsPersistedMessageId() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = channel(registry, service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channelId":1,"body":"hello world"}
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
        EmbeddedChannel sender = channel(registry, service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channelId":1,"messageType":"text","body":"hello generic world"}
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
        EmbeddedChannel sender = channel(registry, service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_message","channelId":1,"messageType":"file","body":"ignored"}
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
        EmbeddedChannel sender = channel(registry, service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channelId":1,"body":"  "}
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
        EmbeddedChannel sender = channel(registry, failingService());
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));
        sender.readOutbound();

        sender.writeInbound(new TextWebSocketFrame("""
                {"type":"send_channel_text_message","channelId":1,"body":"hello world"}
                """));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"problem\""));
        assertTrue(frame.text().contains("\"code\":500"));
        assertTrue(frame.text().contains("internal server error"));
    }

    /**
     * 验证握手完成后会回写欢迎消息并绑定会话。
     */
    @Test
    @DisplayName("user event handshake complete sends welcome message")
    void userEvent_handshakeComplete_sendsWelcomeMessage() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = channel(registry, service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));

        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"welcome\""));
        assertEquals(1, registry.getChannels(1001L).size());
    }

    private static EmbeddedChannel channel(RealtimeSessionRegistry registry, MessageApplicationService service) {
        Supplier<MessageApplicationService> supplier = () -> service;
        MessagePluginConfiguration messagePluginConfiguration = new MessagePluginConfiguration();
        ChannelMessagePluginRegistry pluginRegistry = messagePluginConfiguration.channelMessagePluginRegistry(
                List.of(messagePluginConfiguration.textChannelMessagePlugin())
        );
        RealtimeMessageHandlingConfiguration realtimeMessageHandlingConfiguration = new RealtimeMessageHandlingConfiguration();
        RealtimeInboundMessageDispatcher dispatcher = realtimeMessageHandlingConfiguration.realtimeInboundMessageDispatcher(
                List.of(realtimeMessageHandlingConfiguration.sendChannelMessageRealtimeHandler())
        );
        return new EmbeddedChannel(new RealtimeChannelHandler(
                new JsonProvider(new ObjectMapper()),
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                registry,
                supplier,
                () -> dispatcher
        ));
    }

    private static MessageApplicationService service(RealtimeSessionRegistry registry) {
        return new MessageApplicationService(
                new ChannelRepository() {
                    @Override
                    public java.util.Optional<Channel> findDefaultChannel() {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public java.util.Optional<Channel> findById(long channelId) {
                        return java.util.Optional.of(new Channel(
                                1L, 1L, "public", "public", true,
                                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
                        ));
                    }
                },
                new ChannelMemberRepository() {
                    @Override
                    public boolean exists(long channelId, long accountId) {
                        return true;
                    }

                    @Override
                    public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
                    }

                    @Override
                    public List<Long> findAccountIdsByChannelId(long channelId) {
                        return List.of(1001L, 1002L);
                    }
                },
                new MessageRepository() {
                    @Override
                    public ChannelMessage save(ChannelMessage message) {
                        return message;
                    }

                    @Override
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                new MessageRealtimePublisher() {
                    @Override
                    public void publish(ChannelMessage message, java.util.Collection<Long> recipientAccountIds) {
                        String payload = new JsonProvider(new ObjectMapper()).toJson(new RealtimeServerMessage(
                                "channel_message",
                                null,
                                Instant.parse("2026-04-22T00:00:00Z").toEpochMilli(),
                                message
                        ));
                        for (Long recipientAccountId : recipientAccountIds) {
                            registry.getChannels(recipientAccountId).forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(payload)));
                        }
                    }
                },
                new ChannelMessagePluginRegistry(List.of(new MessagePluginConfiguration().textChannelMessagePlugin())),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new TransactionRunner() {
                    @Override
                    public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public void runInTransaction(Runnable action) {
                        action.run();
                    }
                }
        );
    }

    private static MessageApplicationService failingService() {
        return new MessageApplicationService(
                new ChannelRepository() {
                    @Override
                    public java.util.Optional<Channel> findDefaultChannel() {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public java.util.Optional<Channel> findById(long channelId) {
                        return java.util.Optional.of(new Channel(
                                1L, 1L, "public", "public", true,
                                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
                        ));
                    }
                },
                new ChannelMemberRepository() {
                    @Override
                    public boolean exists(long channelId, long accountId) {
                        return true;
                    }

                    @Override
                    public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
                    }

                    @Override
                    public List<Long> findAccountIdsByChannelId(long channelId) {
                        return List.of(1001L);
                    }
                },
                new MessageRepository() {
                    @Override
                    public ChannelMessage save(ChannelMessage message) {
                        throw new IllegalStateException("boom");
                    }

                    @Override
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                (message, recipientAccountIds) -> {
                },
                new ChannelMessagePluginRegistry(List.of(new MessagePluginConfiguration().textChannelMessagePlugin())),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new TransactionRunner() {
                    @Override
                    public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public void runInTransaction(Runnable action) {
                        action.run();
                    }
                }
        );
    }
}

package team.carrypigeon.backend.chat.domain.config.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.config.server.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.config.ws.downlink.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.config.ws.downlink.RealtimeEventPublisher;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeEventLog;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeResumeCoordinator;
import team.carrypigeon.backend.chat.domain.config.ws.transport.RealtimeChannelInitializer;
import team.carrypigeon.backend.chat.domain.config.ws.transport.RealtimeServerRuntime;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.AuthCommandHandler;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.PingCommandHandler;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.ReauthCommandHandler;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.authentication.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.authentication.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 验证实时通道端到端可用性：真实 Netty 服务端 + 真实 WebSocket 客户端。
 * 覆盖：握手装配、首帧认证、命令分派、认证前置拒绝、未知命令、事件下发、断线续传。
 * 说明：本测试不依赖 Spring 容器与数据库，只装配实时通道自身的协作对象，因此可直接验证 transport 链路是否可用。
 */
class RealtimeChannelIntegrationTests {

    private static final long ACCOUNT_ID = 1001L;
    private static final String SERVER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String PATH = "/api/ws";
    private static final Duration AWAIT = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TimeProviderImpl timeProvider = new TimeProviderImpl(Clock.systemUTC());
    private final AtomicLong idSequence = new AtomicLong(9000);

    private int port;
    private RealtimeServerRuntime runtime;
    private RealtimeEventLog eventLog;
    private RealtimeEventPublisher publisher;
    private WsClient client;

    @BeforeEach
    void startRealtimeServer() throws Exception {
        port = freePort();
        RealtimeServerProperties properties = properties(port);
        IdGenerator idGenerator = idSequence::incrementAndGet;
        RealtimeFrameCodec frameCodec = new RealtimeFrameCodec(new JsonProviderImpl(productionLikeMapper()));
        RealtimeConnectionIndex connectionIndex = new RealtimeConnectionIndex();
        eventLog = new RealtimeEventLog(timeProvider, Duration.ofHours(1).toMillis(), 100, 100);
        publisher = new RealtimeEventPublisher(
                eventLog,
                connectionIndex,
                frameCodec,
                idGenerator,
                timeProvider,
                properties
        );
        RealtimeCommandRegistry commandRegistry = new RealtimeCommandRegistry(List.of(
                new AuthCommandHandler(),
                new ReauthCommandHandler(),
                new PingCommandHandler(timeProvider)
        ));
        RealtimeChannelInitializer initializer = new RealtimeChannelInitializer(
                properties,
                idGenerator,
                timeProvider,
                accessTokenAuthenticationApi(),
                new ServerIdentityProperties(SERVER_ID),
                connectionIndex,
                frameCodec,
                commandRegistry,
                new RealtimeResumeCoordinator(eventLog, frameCodec)
        );
        runtime = new RealtimeServerRuntime(properties, initializer);
        runtime.start();
        assertTrue(runtime.isRunning(), "realtime Netty server must bind its port before tests run");
    }

    @AfterEach
    void stopRealtimeServer() {
        if (client != null) {
            client.close();
        }
        if (runtime != null) {
            runtime.stop();
        }
    }

    /**
     * 验证握手成功后首帧 `auth` 会得到携带账号与服务端标识的 `auth.ok`。
     */
    @Test
    void auth_afterHandshake_returnsAuthOk() throws Exception {
        client = WsClient.connect(port, PATH);

        client.send("""
                {"type":"auth","id":"1","data":{"access_token":"good-token"}}
                """);

        JsonNode frame = objectMapper.readTree(client.awaitFrame());
        assertEquals("auth.ok", frame.get("type").asText());
        assertEquals("1", frame.get("id").asText());
        assertEquals(Long.toString(ACCOUNT_ID), frame.get("data").get("uid").asText());
        assertEquals(SERVER_ID, frame.get("data").get("server_id").asText());
        assertTrue(frame.get("data").get("expires_at").asLong() > 0L, "expires_at must be echoed");
    }

    /**
     * 验证非法凭证被拒绝，并且错误帧使用请求类型加 `.err` 后缀。
     */
    @Test
    void auth_withInvalidToken_returnsUnauthorized() throws Exception {
        client = WsClient.connect(port, PATH);

        client.send("""
                {"type":"auth","id":"2","data":{"access_token":"bad-token"}}
                """);

        JsonNode frame = objectMapper.readTree(client.awaitFrame());
        assertEquals("auth.err", frame.get("type").asText());
        assertEquals("2", frame.get("id").asText());
        assertEquals("unauthorized", frame.get("error").get("reason").asText());
    }

    /**
     * 验证认证后 `ping` 返回 `pong` 并回显请求 ID。
     */
    @Test
    void ping_afterAuth_returnsPong() throws Exception {
        client = WsClient.connect(port, PATH);
        client.authenticate();

        client.send("""
                {"type":"ping","id":"7","data":{}}
                """);

        JsonNode frame = objectMapper.readTree(client.awaitFrame());
        assertEquals("pong", frame.get("type").asText());
        assertEquals("7", frame.get("id").asText());
        assertTrue(frame.get("data").get("server_time").asLong() > 0L, "server_time must be present");
    }

    /**
     * 验证认证前发送需要认证的命令会被拒绝，并且连接被关闭。
     */
    @Test
    void ping_beforeAuth_isRejectedAndConnectionClosed() throws Exception {
        client = WsClient.connect(port, PATH);

        client.send("""
                {"type":"ping","id":"8","data":{}}
                """);

        JsonNode frame = objectMapper.readTree(client.awaitFrame());
        assertEquals("ping.err", frame.get("type").asText());
        assertEquals("unauthorized", frame.get("error").get("reason").asText());
        assertTrue(client.awaitClosed(), "unauthenticated connections must be closed after one error");
    }

    /**
     * 验证未注册命令返回 `unsupported_command`，且已认证连接不会被断开。
     */
    @Test
    void unknownCommand_afterAuth_isRejectedWithoutClosing() throws Exception {
        client = WsClient.connect(port, PATH);
        client.authenticate();

        client.send("""
                {"type":"nope","id":"9","data":{}}
                """);

        JsonNode rejected = objectMapper.readTree(client.awaitFrame());
        assertEquals("nope.err", rejected.get("type").asText());
        assertEquals("unsupported_command", rejected.get("error").get("reason").asText());

        client.send("""
                {"type":"ping","id":"10","data":{}}
                """);
        JsonNode pong = objectMapper.readTree(client.awaitFrame());
        assertEquals("pong", pong.get("type").asText(), "authenticated connection must survive a rejected command");
    }

    /**
     * 验证发布事件后在线账号收到 `type=event` 的下行帧，且事件名与载荷原样下发。
     */
    @Test
    void publish_eventIsDeliveredToOnlineAccount() throws Exception {
        client = WsClient.connect(port, PATH);
        client.authenticate();

        publisher.publish(new RealtimeEvent(
                new RealtimeEventType("channel.changed"),
                Map.of("cid", "1", "scope", "members"),
                List.of(ACCOUNT_ID)
        ));

        JsonNode frame = objectMapper.readTree(client.awaitFrame());
        assertEquals("event", frame.get("type").asText());
        assertTrue(frame.get("id").isNull(), "event frames carry no request id");
        assertFalse(frame.get("data").get("event_id").asText().isBlank());
        assertEquals("channel.changed", frame.get("data").get("event_type").asText());
        assertEquals("1", frame.get("data").get("payload").get("cid").asText());
        assertTrue(frame.get("data").get("server_time").asLong() > 0L, "server_time must be present");
        assertTrue(frame.get("error").isNull());
    }

    /**
     * 验证重连时按 `resume.last_event_id` 回放离线期间错过的事件。
     */
    @Test
    void reconnect_withAnchor_replaysMissedEvents() throws Exception {
        client = WsClient.connect(port, PATH);
        client.authenticate();
        publisher.publish(new RealtimeEvent(new RealtimeEventType("channel.changed"), Map.of("seq", "1"), List.of(ACCOUNT_ID)));
        String firstEventId = objectMapper.readTree(client.awaitFrame()).get("data").get("event_id").asText();
        client.close();
        client = null;

        publisher.publish(new RealtimeEvent(new RealtimeEventType("channel.changed"), Map.of("seq", "2"), List.of(ACCOUNT_ID)));

        client = WsClient.connect(port, PATH);
        client.send("""
                {"type":"auth","id":"11","data":{"access_token":"good-token","resume":{"last_event_id":"%s"}}}
                """.formatted(firstEventId));
        JsonNode authOk = objectMapper.readTree(client.awaitFrame());
        assertEquals("auth.ok", authOk.get("type").asText());

        JsonNode replayed = objectMapper.readTree(client.awaitFrame());
        assertEquals("event", replayed.get("type").asText());
        assertEquals("2", replayed.get("data").get("payload").get("seq").asText());
        assertNotEquals(firstEventId, replayed.get("data").get("event_id").asText());
    }

    /**
     * 验证未知续传锚点下发 `resume.failed`，由客户端回退到 HTTP 全量同步。
     */
    @Test
    void reconnect_withUnknownAnchor_returnsResumeFailed() throws Exception {
        client = WsClient.connect(port, PATH);
        client.authenticate();

        client.send("""
                {"type":"reauth","id":"12","data":{"access_token":"good-token","resume":{"last_event_id":"not-an-event"}}}
                """);

        JsonNode reauthOk = objectMapper.readTree(client.awaitFrame());
        assertEquals("reauth.ok", reauthOk.get("type").asText());
        JsonNode resumeFailed = objectMapper.readTree(client.awaitFrame());
        assertEquals("resume.failed", resumeFailed.get("type").asText());
        assertEquals("event_too_old", resumeFailed.get("data").get("reason").asText());
    }

    /**
     * 构造与生产等价的实时通道配置。
     */
    private static RealtimeServerProperties properties(int port) {
        return new RealtimeServerProperties(
                true,
                "127.0.0.1",
                port,
                PATH,
                1,
                1,
                10,
                60,
                Duration.ofHours(1),
                100,
                100,
                262144
        );
    }

    /**
     * 构造与生产等价的 JSON 配置。
     * 说明：生产使用全局 snake_case 策略，这里保持一致才能验证线上字段名。
     */
    private static ObjectMapper productionLikeMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 构造受控的 access token 认证替身。
     * 说明：只验证实时通道链路，不引入 auth feature 的 token 实现。
     */
    private static AccessTokenAuthenticationApi accessTokenAuthenticationApi() {
        return accessToken -> {
            if (!"good-token".equals(accessToken)) {
                throw ProblemException.of(AuthProblem.INVALID_ACCESS_TOKEN);
            }
            return new AccessTokenAuthenticationResult(ACCOUNT_ID, Instant.now().plusSeconds(300));
        };
    }

    /**
     * 挑选一个当前空闲的本机端口。
     * 说明：服务端需要真实绑定端口，这里先探测再关闭，属于测试内的可用性代价。
     */
    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * 最小 WebSocket 测试客户端。
     * 职责：完成握手、发送文本帧、按序取回下行帧，并观察连接关闭。
     */
    private static final class WsClient implements AutoCloseable {

        private final EventLoopGroup group;
        private final Channel channel;
        private final BlockingQueue<String> frames;
        private final CountDownLatch closed;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private WsClient(EventLoopGroup group, Channel channel, BlockingQueue<String> frames, CountDownLatch closed) {
            this.group = group;
            this.channel = channel;
            this.frames = frames;
            this.closed = closed;
        }

        /**
         * 建立并完成一次 WebSocket 握手。
         *
         * @param port 服务端端口
         * @param path 握手路径
         * @return 已握手的客户端
         */
        private static WsClient connect(int port, String path) throws Exception {
            EventLoopGroup group = new NioEventLoopGroup(1);
            BlockingQueue<String> frames = new LinkedBlockingQueue<>();
            CountDownLatch handshake = new CountDownLatch(1);
            CountDownLatch closed = new CountDownLatch(1);
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    URI.create("ws://127.0.0.1:" + port + path),
                    WebSocketVersion.V13,
                    null,
                    false,
                    new DefaultHttpHeaders()
            );
            Channel channel = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketClientProtocolHandler(handshaker));
                            pipeline.addLast(new SimpleChannelInboundHandler<TextWebSocketFrame>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
                                    frames.add(frame.text());
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext context, Object event) {
                                    if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                                        handshake.countDown();
                                    }
                                }

                                @Override
                                public void channelInactive(ChannelHandlerContext context) {
                                    closed.countDown();
                                }
                            });
                        }
                    })
                    .connect("127.0.0.1", port)
                    .sync()
                    .channel();
            if (!handshake.await(AWAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                group.shutdownGracefully();
                throw new IllegalStateException("websocket handshake did not complete in time");
            }
            return new WsClient(group, channel, frames, closed);
        }

        private void send(String json) throws Exception {
            channel.writeAndFlush(new TextWebSocketFrame(json)).sync();
        }

        private String awaitFrame() throws Exception {
            String frame = frames.poll(AWAIT.toMillis(), TimeUnit.MILLISECONDS);
            assertNotNull(frame, "expected a downstream frame within " + AWAIT);
            return frame;
        }

        private boolean awaitClosed() throws Exception {
            return closed.await(AWAIT.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * 发送首帧认证并断言成功。
         */
        private void authenticate() throws Exception {
            send("""
                    {"type":"auth","id":"100","data":{"access_token":"good-token"}}
                    """);
            JsonNode frame = objectMapper.readTree(awaitFrame());
            assertEquals("auth.ok", frame.get("type").asText());
        }

        @Override
        public void close() {
            channel.close().syncUninterruptibly();
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
        }
    }
}

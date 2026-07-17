package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时通道初始化器。
 * 职责：为每个 Netty 连接建立 HTTP 升级与 WebSocket 文本处理链。
 * 边界：这里只装配协议处理链，不负责服务启动与线程生命周期。
 */
public class RealtimeChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final RealtimeServerProperties properties;
    private final JsonProvider jsonProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final AuthTokenService authTokenService;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final Supplier<ChannelMessagePublishingApi> channelMessagePublishingApiSupplier;
    private final Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier;
    private final boolean requestLogEnabled;

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            ObjectProvider<ChannelMessagePublishingApi> channelMessagePublishingApiProvider,
            ObjectProvider<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherProvider,
            boolean requestLogEnabled
    ) {
        this.properties = properties;
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.authTokenService = authTokenService;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.channelMessagePublishingApiSupplier = channelMessagePublishingApiProvider::getIfAvailable;
        this.realtimeInboundMessageDispatcherSupplier = realtimeInboundMessageDispatcherProvider::getIfAvailable;
        this.requestLogEnabled = requestLogEnabled;
    }

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            ObjectProvider<ChannelMessagePublishingApi> channelMessagePublishingApiProvider,
            ObjectProvider<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherProvider
    ) {
        this(
                properties,
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                serverIdentityProperties,
                realtimeSessionRegistry,
                channelMessagePublishingApiProvider,
                realtimeInboundMessageDispatcherProvider,
                false
        );
    }

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry
    ) {
        this(
                properties,
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                serverIdentityProperties,
                realtimeSessionRegistry,
                new ObjectProvider<>() {
                    @Override
                    public ChannelMessagePublishingApi getObject(Object... args) {
                        return null;
                    }

                    @Override
                    public ChannelMessagePublishingApi getIfAvailable() {
                        return null;
                    }

                    @Override
                    public ChannelMessagePublishingApi getIfUnique() {
                        return null;
                    }

                    @Override
                    public ChannelMessagePublishingApi getObject() {
                        return null;
                    }
                },
                new ObjectProvider<>() {
                    @Override
                    public RealtimeInboundMessageDispatcher getObject(Object... args) {
                        return null;
                    }

                    @Override
                    public RealtimeInboundMessageDispatcher getIfAvailable() {
                        return null;
                    }

                    @Override
                    public RealtimeInboundMessageDispatcher getIfUnique() {
                        return null;
                    }

                    @Override
                    public RealtimeInboundMessageDispatcher getObject() {
                        return null;
                    }
                }
        );
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new RealtimeAccessTokenHandshakeHandler(properties.path(), requestLogEnabled));
        pipeline.addLast(RealtimeChannelHandler.idleStateHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(properties.path()));
        pipeline.addLast(new RealtimeChannelHandler(
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                serverIdentityProperties,
                realtimeSessionRegistry,
                channelMessagePublishingApiSupplier,
                realtimeInboundMessageDispatcherSupplier,
                requestLogEnabled
        ));
    }
}

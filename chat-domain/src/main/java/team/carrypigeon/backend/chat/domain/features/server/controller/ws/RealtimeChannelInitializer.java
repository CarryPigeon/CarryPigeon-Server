package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
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
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final Supplier<MessageApplicationService> messageApplicationServiceSupplier;

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            RealtimeSessionRegistry realtimeSessionRegistry,
            ObjectProvider<MessageApplicationService> messageApplicationServiceProvider
    ) {
        this.properties = properties;
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.authTokenService = authTokenService;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.messageApplicationServiceSupplier = messageApplicationServiceProvider::getIfAvailable;
    }

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            RealtimeSessionRegistry realtimeSessionRegistry
    ) {
        this(
                properties,
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                realtimeSessionRegistry,
                new ObjectProvider<>() {
                    @Override
                    public MessageApplicationService getObject(Object... args) {
                        return null;
                    }

                    @Override
                    public MessageApplicationService getIfAvailable() {
                        return null;
                    }

                    @Override
                    public MessageApplicationService getIfUnique() {
                        return null;
                    }

                    @Override
                    public MessageApplicationService getObject() {
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
        pipeline.addLast(new RealtimeAccessTokenHandshakeHandler(properties.path(), authTokenService));
        pipeline.addLast(RealtimeChannelHandler.idleStateHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(properties.path()));
        pipeline.addLast(new RealtimeChannelHandler(
                jsonProvider,
                idGenerator,
                timeProvider,
                realtimeSessionRegistry,
                messageApplicationServiceSupplier
        ));
    }
}

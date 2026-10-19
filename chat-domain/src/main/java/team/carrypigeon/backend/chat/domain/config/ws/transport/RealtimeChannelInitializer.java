package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.config.server.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.config.ws.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.config.ws.downlink.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeResumeCoordinator;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时通道初始化器。
 * 职责：为每个 Netty 连接建立 HTTP 升级、会话上下文与 WebSocket 文本处理链。
 * 边界：只装配协议处理链，不负责服务启动与线程生命周期，也不承载命令语义。
 */
@Component
public class RealtimeChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final RealtimeServerProperties properties;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final AccessTokenAuthenticationApi accessTokenAuthenticationApi;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeConnectionIndex realtimeConnectionIndex;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeCommandRegistry commandRegistry;
    private final RealtimeResumeCoordinator resumeCoordinator;

    public RealtimeChannelInitializer(
            RealtimeServerProperties properties,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec frameCodec,
            RealtimeCommandRegistry commandRegistry,
            RealtimeResumeCoordinator resumeCoordinator
    ) {
        this.properties = properties;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.accessTokenAuthenticationApi = accessTokenAuthenticationApi;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeConnectionIndex = realtimeConnectionIndex;
        this.frameCodec = frameCodec;
        this.commandRegistry = commandRegistry;
        this.resumeCoordinator = resumeCoordinator;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new RealtimeChannelSession(properties.path()));
        pipeline.addLast(RealtimeChannelHandler.idleStateHandler(properties.readIdleTimeoutSeconds()));
        pipeline.addLast(new WebSocketServerProtocolHandler(properties.path()));
        pipeline.addLast(new RealtimeChannelHandler(
                idGenerator,
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeConnectionIndex,
                frameCodec,
                commandRegistry,
                resumeCoordinator,
                properties.authenticationTimeoutSeconds()
        ));
    }
}

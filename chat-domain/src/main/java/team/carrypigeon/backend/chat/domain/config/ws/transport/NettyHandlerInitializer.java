package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.config.ws.transport.handlers.NettyRealtimeContextHandler;
import team.carrypigeon.backend.chat.domain.config.ws.transport.handlers.NettyRealtimeServiceHandler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeServerProperties;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时通道初始化器。
 * 职责：为每个 Netty 连接建立 HTTP 升级、会话上下文与 WebSocket 文本处理链。
 * 边界：只装配协议处理链，不负责服务启动与线程生命周期，也不承载命令语义。
 */
@Component
public class NettyHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private final RealtimeServerProperties properties;
    private final TimeProviderImpl timeProvider;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeCommandRegistry commandRegistry;
    private final RealtimeAuthState realtimeAuthState;

    public NettyHandlerInitializer(
        RealtimeServerProperties properties,
        TimeProviderImpl timeProvider,
        RealtimeFrameCodec frameCodec,
        RealtimeCommandRegistry commandRegistry,
        RealtimeAuthState realtimeAuthState
    ) {
        this.properties = properties;
        this.timeProvider = timeProvider;
        this.realtimeAuthState = realtimeAuthState;
        this.frameCodec = frameCodec;
        this.commandRegistry = commandRegistry;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        // http解码
        pipeline.addLast(new HttpServerCodec());
        // http消息聚合
        pipeline.addLast(new HttpObjectAggregator(65536));
        // 升级为WebSocket
        pipeline.addLast(new WebSocketServerProtocolHandler("/api/ws"));
        // 空闲检测
        pipeline.addLast(NettyRealtimeServiceHandler.idleStateHandler(properties.getReadIdleTimeoutSeconds()));
        // WebSocket处理
        // 上下文注入
        pipeline.addLast(new NettyRealtimeContextHandler());
        // 事件处理
        pipeline.addLast(new NettyRealtimeServiceHandler(
            frameCodec,
            commandRegistry,
            realtimeAuthState,
            timeProvider,
            properties.getAuthenticationTimeoutSeconds()
        ));
    }
}

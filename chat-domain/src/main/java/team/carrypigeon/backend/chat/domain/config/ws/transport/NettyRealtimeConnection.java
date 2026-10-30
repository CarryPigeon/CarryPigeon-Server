package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;

/**
 * 实时命令作用域实现。
 * 职责：把一次命令处理绑定到当前连接、当前命令与回复出口，并实现内建命令所需的认证与续传能力。
 * 边界：只做转发与绑定；认证判断、续传策略仍分别由认证状态机与续传协调器承担。
 */
public final class NettyRealtimeConnection implements RealtimeConnection {

    private final ChannelHandlerContext context;

    public NettyRealtimeConnection(
            ChannelHandlerContext context
    ) {
        this.context = context;
    }

    @Override
    public void send(String content) {
        context.writeAndFlush(new TextWebSocketFrame(content));
    }

    @Override
    public boolean isActive() {
        return context.channel().isActive();
    }

    @Override
    public boolean isWritable() {
        return context.channel().isWritable();
    }

    @Override
    public void close() {
        context.close();
    }
}

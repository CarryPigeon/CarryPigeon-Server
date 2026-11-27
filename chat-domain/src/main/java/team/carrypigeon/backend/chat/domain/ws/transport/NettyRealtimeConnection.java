package team.carrypigeon.backend.chat.domain.ws.transport;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeConnection;

/**
 * 实时命令作用域实现。
 * 职责：把一次命令处理绑定到当前连接、当前命令与回复出口，并实现内建命令所需的认证与续传能力。
 * 边界：只做转发与绑定；认证判断、续传策略仍分别由认证状态机与续传协调器承担。
 */
@Slf4j
public final class NettyRealtimeConnection implements RealtimeConnection {

    private final ChannelHandlerContext context;

    public NettyRealtimeConnection(
            ChannelHandlerContext context
    ) {
        this.context = context;
    }

    @Override
    public void send(String content) {
        write(context, content);
    }

    /** 所有文本帧共用写出边界。监听器可在其他线程执行，使用发起写入时的上下文。 */
    public static ChannelFuture write(ChannelHandlerContext context, String content) {
        var snapshot = LogContexts.snapshot();
        ChannelFuture future = context.writeAndFlush(new TextWebSocketFrame(content));
        future.addListener((ChannelFutureListener) completed -> {
            if (!completed.isSuccess()) {
                try (var ignored = LogContexts.openScope()) {
                    LogContexts.putAll(snapshot);
                    LogContexts.put(LogKeys.CONNECTION_ID, context.channel().id().asLongText());
                    log.atWarn()
                        .addKeyValue("stage", "frame_write")
                        .setCause(completed.cause())
                        .log("实时帧写出失败");
                }
            }
        });
        return future;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NettyRealtimeConnection other)) return false;
        return context.channel().equals(other.context.channel());
    }

    @Override
    public int hashCode() {
        return context.channel().hashCode();
    }
}

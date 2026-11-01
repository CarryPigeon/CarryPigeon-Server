package team.carrypigeon.backend.chat.domain.config.ws.transport.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.config.ws.transport.NettyRealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogScope;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;

import java.util.UUID;

/**
 * WebSocket 帧日志上下文处理器。
 * 职责：为每个上行帧建立独立的日志作用域，并写入通道、用户、帧标识与路由信息。
 * 边界：WS 的 {@code session_id} 表示单帧标识，{@code trace_id} 与该帧 {@code session_id} 相同，不做跨帧串联。
 */
public class NettyRealtimeContextHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        RealtimeAttributes attributes = new NettyRealtimeAttributes(ctx.channel());
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            Long accountId = attributes.getAccountId();
            if (accountId != null) {
                LogContexts.uid(String.valueOf(accountId));
            }
            LogContexts.route("websocket");
            String frameId = UUID.randomUUID().toString();
            LogContexts.sessionId(frameId);
            LogContexts.traceId(frameId);
            ctx.fireChannelRead(msg.retain());
        }
    }
}

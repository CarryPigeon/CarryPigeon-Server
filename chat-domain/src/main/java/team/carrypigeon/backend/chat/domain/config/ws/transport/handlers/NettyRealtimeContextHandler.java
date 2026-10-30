package team.carrypigeon.backend.chat.domain.config.ws.transport.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.config.ws.transport.NettyRealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;

import java.util.UUID;

/**
 * 上下处理器，为一个事件注入上下文
 * 包含用户id,request id,trace id，以及路由信息
 *
 */
public class NettyRealtimeContextHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        RealtimeAttributes attributes = new NettyRealtimeAttributes(ctx.channel());
        try (LogContexts.Scope ignored = LogContexts.openScope()) {
            Long accountId = attributes.getAccountId();
            if (accountId != null) {
                LogContexts.uid(String.valueOf(accountId));
            }
            LogContexts.route("websocket");
            String id = UUID.randomUUID().toString();
            LogContexts.traceId(id);
            String sessionId = attributes.getSessionId();
            if (sessionId==null){
                sessionId = UUID.randomUUID().toString();
                attributes.setSessionId(sessionId);
            }
            LogContexts.sessionId(sessionId);
            ctx.fireChannelRead(msg.retain());
        }
    }
}

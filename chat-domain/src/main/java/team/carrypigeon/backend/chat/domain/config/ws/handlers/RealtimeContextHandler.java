package team.carrypigeon.backend.chat.domain.config.ws.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.config.ws.context.RealtimeContext;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

import java.util.UUID;

/**
 * 上下处理器，为一个事件注入上下文
 * 包含用户id,request id,trace id，以及路由信息
 *
 */
public class RealtimeContextHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        try (LogContexts.Scope ignored = LogContexts.openScope()) {
            CpPrincipal principal = RealtimeContext.getAuthenticatedPrincipalKey(ctx.channel());
            if (principal != null) {
                LogContexts.uid(String.valueOf(principal.accountId()));
            }
            LogContexts.route("websocket");
            String id = UUID.randomUUID().toString();
            LogContexts.traceId(id);
            String sessionId = RealtimeContext.getSessionIdKey(ctx.channel());
            if (sessionId==null){
                sessionId = UUID.randomUUID().toString();
                RealtimeContext.setSessionIdKey(ctx.channel(), sessionId);
            }
            LogContexts.sessionId(sessionId);
            ctx.fireChannelRead(msg.retain());
        }
    }
}

package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * 实时通道 MDC 边界。
 * 职责：在一次入站处理或定时任务期间建立通道日志上下文，并保证结束后清理。
 * 边界：不跨线程保存 MDC，也不处理协议或业务命令。
 */
final class RealtimeChannelLogContext {

    private RealtimeChannelLogContext() {
    }

    static void run(ChannelHandlerContext context, Runnable action) {
        CpPrincipal principal = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                .get();
        try (LogContexts.Scope ignored = LogContexts.openScope()) {
            LogContexts.traceId(context.channel().attr(RealtimeChannelSession.TRACE_ID_KEY).get());
            LogContexts.requestId(context.channel().attr(RealtimeChannelSession.REQUEST_ID_KEY).get());
            LogContexts.route(context.channel().attr(RealtimeChannelSession.ROUTE_KEY).get());
            if (principal != null) {
                LogContexts.uid(Long.toString(principal.accountId()));
            }
            action.run();
        }
    }
}

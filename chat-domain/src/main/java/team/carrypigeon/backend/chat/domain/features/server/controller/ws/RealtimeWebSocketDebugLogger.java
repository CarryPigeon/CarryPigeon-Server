package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;

/**
 * 实时 WebSocket 本地调试日志器。
 * 职责：在本地请求日志开关开启时输出 WS 握手与连接生命周期摘要。
 * 边界：不记录请求体、消息正文、Cookie、Authorization 或 access token。
 */
final class RealtimeWebSocketDebugLogger {

    private static final Logger log = LoggerFactory.getLogger(RealtimeWebSocketDebugLogger.class);
    private static final RealtimeWebSocketDebugLogger DISABLED = new RealtimeWebSocketDebugLogger(false);

    private final boolean enabled;

    RealtimeWebSocketDebugLogger(boolean enabled) {
        this.enabled = enabled;
    }

    static RealtimeWebSocketDebugLogger disabled() {
        return DISABLED;
    }

    void handshakeRequest(ChannelHandlerContext context, FullHttpRequest request, String expectedPath, boolean matched) {
        if (!enabled) {
            return;
        }
        log.info("Action: local_ws_handshake_request_received"
                + " method=" + request.method().name()
                + " uri=" + sanitizeUri(request.uri())
                + " expectedPath=" + expectedPath
                + " matched=" + matched
                + " origin=" + safeHeader(request, "Origin")
                + " upgrade=" + safeHeader(request, "Upgrade")
                + " remoteAddr=" + remoteAddress(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void handshakeComplete(ChannelHandlerContext context, WebSocketServerProtocolHandler.HandshakeComplete event) {
        if (!enabled) {
            return;
        }
        log.info("Action: local_ws_handshake_completed"
                + " uri=" + sanitizeUri(event.requestUri())
                + " selectedSubprotocol=" + blankIfNull(event.selectedSubprotocol())
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void frameReceived(ChannelHandlerContext context, RealtimeClientMessage request, int textLength) {
        if (!enabled) {
            return;
        }
        log.info("Action: local_ws_frame_received"
                + " type=" + blankIfNull(request == null ? null : request.type())
                + " id=" + blankIfNull(request == null ? null : request.id())
                + " textLength=" + textLength
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void frameRejected(ChannelHandlerContext context, String reason, Throwable failure) {
        if (!enabled) {
            return;
        }
        log.info("Action: local_ws_frame_rejected"
                + " reason=" + blankIfNull(reason)
                + " failure=" + failureName(failure)
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void authResult(ChannelHandlerContext context, RealtimeClientMessage request, boolean reauth, boolean success, String reason) {
        if (!enabled) {
            return;
        }
        AuthenticatedAccount principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        log.info("Action: local_ws_auth_result"
                + " type=" + (reauth ? "reauth" : "auth")
                + " id=" + blankIfNull(request == null ? null : request.id())
                + " success=" + success
                + " reason=" + blankIfNull(reason)
                + " accountId=" + (principal == null ? "" : principal.accountId())
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void channelInactive(ChannelHandlerContext context) {
        if (!enabled) {
            return;
        }
        AuthenticatedAccount principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        log.info("Action: local_ws_channel_inactive"
                + " accountId=" + (principal == null ? "" : principal.accountId())
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    void exceptionCaught(ChannelHandlerContext context, Throwable failure) {
        if (!enabled) {
            return;
        }
        log.info("Action: local_ws_channel_exception"
                + " failure=" + failureName(failure)
                + " message=" + sanitizeValue(failure == null ? "" : failure.getMessage())
                + " remoteAddr=" + remoteAddress(context)
                + " sessionId=" + sessionId(context)
                + " requestId=" + requestId(context)
                + " traceId=" + traceId(context));
    }

    String sanitizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        int queryIndex = uri.indexOf('?');
        if (queryIndex < 0) {
            return sanitizeValue(uri);
        }
        String path = uri.substring(0, queryIndex);
        String query = uri.substring(queryIndex + 1);
        return sanitizeValue(path + "?" + sanitizeQuery(query));
    }

    String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query.replaceAll("(?i)(access_token|refresh_token|token|password|secret|code)=([^&]*)", "$1=***");
    }

    /**
     * 安全读取握手请求头用于调试日志。
     * 约束：请求头值必须先清理控制字符，避免日志换行注入。
     *
     * @param request WebSocket 握手请求
     * @param name 请求头名称
     * @return 可写入日志的请求头值
     */
    private String safeHeader(FullHttpRequest request, String name) {
        String value = request.headers().get(name);
        return sanitizeValue(value);
    }

    private String requestId(ChannelHandlerContext context) {
        return blankIfNull(context.channel().attr(RealtimeChannelSession.REQUEST_ID_KEY).get());
    }

    private String traceId(ChannelHandlerContext context) {
        return blankIfNull(context.channel().attr(RealtimeChannelSession.TRACE_ID_KEY).get());
    }

    private String sessionId(ChannelHandlerContext context) {
        return blankIfNull(context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get());
    }

    /**
     * 解析可写入日志的远端地址。
     * 约束：远端地址也经过清理，避免异常地址文本污染日志结构。
     *
     * @param context Netty 通道上下文
     * @return 可写入日志的远端地址
     */
    private String remoteAddress(ChannelHandlerContext context) {
        return context.channel().remoteAddress() == null ? "" : sanitizeValue(context.channel().remoteAddress().toString());
    }

    private String failureName(Throwable failure) {
        return failure == null ? "" : failure.getClass().getSimpleName();
    }

    private String blankIfNull(String value) {
        return value == null ? "" : sanitizeValue(value);
    }

    /**
     * 清理将写入 WebSocket 调试日志的文本值。
     * 约束：去除换行、回车和制表符，避免用户输入破坏单行结构化日志。
     *
     * @param value 原始文本
     * @return 可安全写入日志的文本
     */
    private String sanitizeValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
    }
}

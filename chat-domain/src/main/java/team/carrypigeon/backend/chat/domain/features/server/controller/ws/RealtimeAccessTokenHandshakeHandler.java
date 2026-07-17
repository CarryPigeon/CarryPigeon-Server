package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.UUID;
import team.carrypigeon.backend.chat.domain.shared.controller.support.HttpRequestMdcFilter;

/**
 * WebSocket 握手上下文准备处理器。
 * 职责：在 WebSocket 升级前准备请求链路上下文，认证改由首帧 `auth` 承接。
 * 边界：这里只处理升级前路由与日志上下文，不再做握手期 Bearer 鉴权。
 */
public class RealtimeAccessTokenHandshakeHandler extends ChannelInboundHandlerAdapter {

    private final String path;
    private final RealtimeWebSocketDebugLogger debugLogger;

    public RealtimeAccessTokenHandshakeHandler(String path) {
        this(path, false);
    }

    public RealtimeAccessTokenHandshakeHandler(String path, boolean requestLogEnabled) {
        this.path = path;
        this.debugLogger = requestLogEnabled
                ? new RealtimeWebSocketDebugLogger(true)
                : RealtimeWebSocketDebugLogger.disabled();
    }

    /**
     * 在握手升级请求进入后写入请求级上下文属性。
     * 输入：Netty 握手请求或后续管道消息。
     * 副作用：为匹配的 WS 路由记录 requestId、traceId 与 route 属性。
     *
     * @param context Netty 通道上下文
     * @param message 当前入站消息
     */
    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        if (!(message instanceof FullHttpRequest request)) {
            context.fireChannelRead(message);
            return;
        }
        if (!matchesPath(request.uri())) {
            debugLogger.handshakeRequest(context, request, path, false);
            context.fireChannelRead(message);
            return;
        }

        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        context.channel().attr(RealtimeChannelSession.REQUEST_ID_KEY).set(requestId);
        context.channel().attr(RealtimeChannelSession.TRACE_ID_KEY).set(traceId);
        context.channel().attr(RealtimeChannelSession.ROUTE_KEY).set(path);
        debugLogger.handshakeRequest(context, request, path, true);
        context.fireChannelRead(message);
    }

    /**
     * 记录握手阶段异常并交给后续处理器继续处理。
     *
     * @param context Netty 通道上下文
     * @param cause 当前异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        debugLogger.exceptionCaught(context, cause);
        super.exceptionCaught(context, cause);
    }

    /**
     * 判断当前 HTTP 升级请求是否匹配实时通道路由。
     * 约束：匹配时忽略 query string，只比较请求路径。
     *
     * @param uri 握手请求 URI
     * @return 路径匹配时返回 true
     */
    private boolean matchesPath(String uri) {
        int queryIndex = uri.indexOf('?');
        String requestPath = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
        return path.equals(requestPath);
    }

    /**
     * 解析或生成握手请求 ID。
     * 语义：客户端未提供 `X-Request-Id` 时生成服务端请求 ID，供后续 WS 通道日志复用。
     *
     * @param request WebSocket 握手请求
     * @return 请求 ID
     */
    private String resolveRequestId(FullHttpRequest request) {
        String requestId = request.headers().get(HttpRequestMdcFilter.REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    /**
     * 解析握手链路追踪 ID。
     * 语义：客户端未提供 `X-Trace-Id` 时复用 requestId，保证 WS 通道有稳定 trace 标识。
     *
     * @param request WebSocket 握手请求
     * @param requestId 已解析的请求 ID
     * @return trace ID
     */
    private String resolveTraceId(FullHttpRequest request, String requestId) {
        String traceId = request.headers().get(HttpRequestMdcFilter.TRACE_ID_HEADER);
        return traceId == null || traceId.isBlank() ? requestId : traceId;
    }
}

package team.carrypigeon.backend.chat.domain.config.ws.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.config.http.security.CpPrincipal;
import team.carrypigeon.backend.chat.domain.shared.controller.support.HttpRequestMdcFilter;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogValueSanitizer;

/**
 * 实时连接会话上下文。
 * 职责：声明会话属性键，在 WebSocket 升级请求进入时写入请求级属性，并在一次处理期间建立通道日志上下文。
 * 边界：只维护通道属性与日志边界，不解析上行帧、不执行认证或业务命令。
 */
@Slf4j
public class RealtimeChannelSession extends ChannelInboundHandlerAdapter {

    /** 会话 ID。 */
    public static final AttributeKey<String> SESSION_ID_KEY = AttributeKey.valueOf("cp.realtime.session-id");

    /** 链路追踪 ID。 */
    public static final AttributeKey<String> TRACE_ID_KEY = AttributeKey.valueOf("cp.realtime.trace-id");

    /** 请求 ID。 */
    public static final AttributeKey<String> REQUEST_ID_KEY = AttributeKey.valueOf("cp.realtime.request-id");

    /** 实时通道路由。 */
    public static final AttributeKey<String> ROUTE_KEY = AttributeKey.valueOf("cp.realtime.route");

    /** 客户端声明的最后事件 ID。 */
    public static final AttributeKey<String> LAST_EVENT_ID_KEY = AttributeKey.valueOf("cp.realtime.last-event-id");

    /** 已认证主体。 */
    public static final AttributeKey<CpPrincipal> AUTHENTICATED_PRINCIPAL_KEY =
            AttributeKey.valueOf("cp.realtime.authenticated-principal");

    /** 认证超时任务。 */
    public static final AttributeKey<ScheduledFuture<?>> AUTHENTICATION_TIMEOUT_FUTURE_KEY =
            AttributeKey.valueOf("cp.realtime.authentication-timeout-future");

    /** access token 过期时间。 */
    public static final AttributeKey<Instant> ACCESS_TOKEN_EXPIRES_AT_KEY =
            AttributeKey.valueOf("cp.realtime.access-token-expires-at");

    /** access token 过期任务。 */
    public static final AttributeKey<ScheduledFuture<?>> ACCESS_TOKEN_EXPIRATION_FUTURE_KEY =
            AttributeKey.valueOf("cp.realtime.access-token-expiration-future");
    private final String path;

    public RealtimeChannelSession(String path) {
        this.path = path;
    }

    /**
     * 在握手升级请求进入后写入请求级上下文属性。
     * 输入：Netty 握手请求或后续管道消息。
     * 副作用：为匹配的实时通道路由记录 requestId、traceId 与 route 属性。
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
            context.fireChannelRead(message);
            return;
        }
        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        context.channel().attr(REQUEST_ID_KEY).set(requestId);
        context.channel().attr(TRACE_ID_KEY).set(traceId);
        context.channel().attr(ROUTE_KEY).set(path);
        if (log.isDebugEnabled()) {
            log.debug("Realtime handshake request, uri={}, requestId={}, traceId={}",
                    LogValueSanitizer.uri(request.uri()), requestId, traceId);
        }
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
        log.debug("Realtime handshake pipeline failed", cause);
        super.exceptionCaught(context, cause);
    }

    /**
     * 在一次入站处理或定时任务期间建立通道日志上下文，并保证结束后清理。
     * 约束：不跨线程保存 MDC；调用方必须在本方法内部完成整段处理。
     *
     * @param context Netty 通道上下文
     * @param action 需要处于日志上下文中的动作
     */
    static void run(ChannelHandlerContext context, Runnable action) {
        CpPrincipal principal = context.channel().attr(AUTHENTICATED_PRINCIPAL_KEY).get();
        try (LogContexts.Scope ignored = LogContexts.openScope()) {
            LogContexts.traceId(context.channel().attr(TRACE_ID_KEY).get());
            LogContexts.requestId(context.channel().attr(REQUEST_ID_KEY).get());
            LogContexts.route(context.channel().attr(ROUTE_KEY).get());
            if (principal != null) {
                LogContexts.uid(Long.toString(principal.accountId()));
            }
            action.run();
        }
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
     * 语义：客户端未提供请求 ID 时生成服务端请求 ID，供后续通道日志复用。
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
     * 语义：客户端未提供链路 ID 时复用 requestId，保证通道有稳定 trace 标识。
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

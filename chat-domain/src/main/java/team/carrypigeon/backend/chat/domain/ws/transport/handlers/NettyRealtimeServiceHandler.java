package team.carrypigeon.backend.chat.domain.ws.transport.handlers;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import team.carrypigeon.backend.chat.domain.ws.transport.NettyRealtimeAttributes;
import team.carrypigeon.backend.chat.domain.ws.transport.NettyRealtimeConnection;
import team.carrypigeon.backend.chat.domain.ws.transport.NettyRealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogScope;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;
import team.carrypigeon.backend.chat.domain.ws.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeAuthState;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrameCodec;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeProtocolException;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeAttributes;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeConnection;
import team.carrypigeon.backend.chat.domain.ws.connection.RealtimeScheduler;
import team.carrypigeon.backend.chat.domain.ws.frame.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.ws.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.ws.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 实时通道处理器。
 * 职责：承接 WebSocket 连接生命周期，并按命令注册表分派上行命令。
 * 边界：认证机制、续传与帧编解码由聚焦协作对象承担；聊天写入只走 HTTP。
 */
@Slf4j
public class NettyRealtimeServiceHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // 连接的存储
    /** 连接。 */
    private static final AttributeKey<RealtimeConnection> CONNECTION_KEY =
        AttributeKey.valueOf("cp.realtime.connection");

    /** 规划器。 */
    private static final AttributeKey<RealtimeScheduler> SCHEDULER_KEY =
        AttributeKey.valueOf("cp.realtime.scheduler");

    /** 存储器 */
    private static final AttributeKey<RealtimeAttributes> ATTRIBUTES_KEY =
        AttributeKey.valueOf("cp.realtime.attributes");

    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAuthState realtimeAuthState;
    private final RealtimeCommandRegistry commandRegistry;
    private final TimeProvider timeProvider;
    private final int authenticationTimeoutSeconds;
    private final boolean requestLogEnabled;

    public NettyRealtimeServiceHandler(
        RealtimeFrameCodec frameCodec,
        RealtimeCommandRegistry commandRegistry,
        RealtimeAuthState realtimeAuthState,
        TimeProvider timeProvider,
        int authenticationTimeoutSeconds, boolean requestLogEnabled
    ) {
        this.requestLogEnabled = requestLogEnabled;
        this.frameCodec = frameCodec;
        this.realtimeAuthState = realtimeAuthState;
        this.commandRegistry = commandRegistry;
        this.timeProvider = timeProvider;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            LogContexts.put(LogKeys.CONNECTION_ID, context.channel().id().asLongText());
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                log.debug("WebSocket 握手完成");
                RealtimeAttributes attributes = resolveAttributes(context);
                resolveScheduler(context, attributes).scheduleAuthenticationTimeout(authenticationTimeoutSeconds);
                return;
            }
            if (event instanceof IdleStateEvent) {
                //TODO 错误码规范
                log.debug("WebSocket 连接空闲超时");
                writeFrame(context, new RealtimeFrame.CommandError(
                    null,
                    RealtimeFrameCodec.COMMAND_FRAME_TYPE,
                    11000,
                    "realtime connection is idle"
                )).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            super.userEventTriggered(context, event);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        long startedAt = requestLogEnabled ? System.nanoTime() : 0;
        RealtimeClientMessage request;
        try {
            request = frameCodec.decode(frame.text());
        } catch (ProblemException invalidFrame) {
            // 保留解码失败直接断连的协议行为，尚无可回显的命令 ID。
            logSummary(startedAt, null, "invalid_frame");
            context.close();
            return;
        }
        RealtimeConnection connection = resolveConnection(context);
        RealtimeAttributes attributes = resolveAttributes(context);
        RealtimeScheduler scheduler = resolveScheduler(context, attributes);
        Integer code = null;
        String reason = null;
        try {
            if (request == null) {
                throw new RealtimeProtocolException("frame_invalid", "frame must be an object");
            }
            if (LogContexts.usableId(request.id())) {
                LogContexts.sessionId(request.id());
                LogContexts.traceId(request.id());
            }
            if (request.type() == null || request.type().isBlank()) {
                throw new RealtimeProtocolException("type_blank", "type must not be blank");
            }
            Optional<RealtimeCommandHandler> handler = commandRegistry.find(request.type());
            if (handler.isEmpty()) {
                code = 400;
                writeErrorAndCloseIfUnauthenticated(context, attributes, request, code, "unsupported realtime command");
                return;
            }
            LogContexts.route(handler.get().commandType());
            if (handler.get().requiresAuthentication() && attributes.getAccountId() == null) {
                code = 401;
                writeErrorAndCloseIfUnauthenticated(context, attributes, request, code, "auth must be the first realtime command");
                return;
            }
            handler.get().handle(request, connection, attributes, scheduler, realtimeAuthState);
        } catch (RuntimeException exception) {
            if (exception instanceof ProblemException problem) {
                code = problem.mapping().code();
                if (code == 10011 || code == 10012) {
                    log.atError()
                        .addKeyValue("business_code", code)
                        .addKeyValue("reason", problem.mapping())
                        .addKeyValue("failure_type", NestedExceptionUtils.getMostSpecificCause(problem).getClass().getName())
                        .log("实时命令依赖或内部操作失败");
                }
            } else {
                code = 500;
                if (exception instanceof RealtimeProtocolException protocol) {
                    reason = protocol.reason();
                } else {
                    log.error("实时命令处理异常", exception);
                }
            }
            writeRealtimeFailure(context, attributes, request, exception);
        } finally {
            logSummary(startedAt, code, reason);
        }
    }

    /** 计时仅覆盖命令处理，异步写入失败由传输层另行记录。 */
    private void logSummary(long startedAt, Integer code, String reason) {
        if (requestLogEnabled) {
            var event = log.atInfo()
                .addKeyValue("action", "ws.command")
                .addKeyValue("outcome", code == null && reason == null ? "success" : "failure")
                .addKeyValue("duration_ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
            if (code != null) {
                event.addKeyValue("business_code", code);
            }
            if (reason != null) {
                event.addKeyValue("reason", reason);
            }
            event.log("实时命令处理结束");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            LogContexts.put(LogKeys.CONNECTION_ID, context.channel().id().asLongText());
            RealtimeConnection connection = resolveConnection(context);
            RealtimeAttributes attributes = resolveAttributes(context);
            RealtimeScheduler scheduler = resolveScheduler(context, attributes);
            Long accountId = attributes.getAccountId();
            realtimeAuthState.onChannelInactive(connection,attributes,scheduler);
            LogContexts.uid(accountId == null ? null : accountId.toString());
            log.debug("WebSocket 连接已关闭");
            super.channelInactive(context);
        }
    }

    /**
     * 按失败语义写出实时错误帧。
     * 约定：协议错误与业务拒绝使用异常自身原因；未预期异常才使用 internal_server_error。
     *
     * @param context Netty 上下文
     * @param attributes 当前连接属性
     * @param request 当前帧请求，可能为空
     * @param exception 待处理的异常
     */
    private void writeRealtimeFailure(
            ChannelHandlerContext context,
            RealtimeAttributes attributes,
            RealtimeClientMessage request,
            RuntimeException exception
    ) {
        if (exception instanceof ProblemException problemException) {
            writeErrorAndCloseIfUnauthenticated(
                    context, attributes, request,
                    problemException.mapping().code(), safeMessage(problemException.getMessage()));
            return;
        }
        writeErrorAndCloseIfUnauthenticated(
                context, attributes, request, 500, "internal server error");
    }

    private RealtimeConnection resolveConnection(ChannelHandlerContext context) {
        RealtimeConnection connection = context.channel().attr(CONNECTION_KEY).get();
        if (connection == null) {
            connection = new NettyRealtimeConnection(context);
            context.channel().attr(CONNECTION_KEY).set(connection);
        }
        return connection;
    }

    private RealtimeAttributes resolveAttributes(ChannelHandlerContext context) {
        RealtimeAttributes attributes = context.channel().attr(ATTRIBUTES_KEY).get();
        if (attributes == null) {
            attributes = new NettyRealtimeAttributes(context.channel());
            context.channel().attr(ATTRIBUTES_KEY).set(attributes);
        }
        return attributes;
    }

    private RealtimeScheduler resolveScheduler(ChannelHandlerContext context, RealtimeAttributes attributes) {
        RealtimeScheduler scheduler = context.channel().attr(SCHEDULER_KEY).get();
        if (scheduler == null) {
            scheduler = new NettyRealtimeScheduler(context, frameCodec, attributes, timeProvider);
            context.channel().attr(SCHEDULER_KEY).set(scheduler);
        }
        return scheduler;
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "request failed" : message;
    }

    /**
     * 记录异常并关闭当前实时连接。
     * 副作用：写入异常日志并主动关闭 Netty 通道，避免异常连接继续留在会话表中。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            LogContexts.put(LogKeys.CONNECTION_ID, context.channel().id().asLongText());
            log.warn("WebSocket 连接异常", cause);
        }
        context.close();
    }

    /**
     * 创建实时连接读空闲检测处理器。
     * 输出：指定时间无入站数据即触发空闲事件，供本处理器关闭连接。
     *
     * @param readIdleTimeoutSeconds 读空闲秒数
     * @return Netty 空闲检测处理器
     */
    public static IdleStateHandler idleStateHandler(int readIdleTimeoutSeconds) {
        if (readIdleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("readIdleTimeoutSeconds must be greater than 0");
        }
        return new IdleStateHandler(readIdleTimeoutSeconds, 0, 0, TimeUnit.SECONDS);
    }

    /**
     * 写出命令错误帧，并在连接未认证时关闭。
     * 约束：未认证连接只有一次出错机会，写完后必须关闭，避免匿名连接长期占用。
     */
    private void writeErrorAndCloseIfUnauthenticated(
        ChannelHandlerContext context,
        RealtimeAttributes attributes,
        RealtimeClientMessage request,
        int code,
        String message
    ) {
        if (attributes.getAccountId() != null) {
            writeFrame(context, commandError(request, code, message));
            return;
        }
        writeFrame(context, commandError(request, code, message)).addListener(ChannelFutureListener.CLOSE);
    }

    private RealtimeFrame.CommandError commandError(RealtimeClientMessage request, int code, String message) {
        return new RealtimeFrame.CommandError(
            request == null ? null : request.id(),
            request == null ? RealtimeFrameCodec.COMMAND_FRAME_TYPE : request.type(),
            code,
            message
        );
    }

    private ChannelFuture writeFrame(ChannelHandlerContext context, RealtimeFrame frame) {
        return NettyRealtimeConnection.write(context, frameCodec.encode(frame));
    }
}

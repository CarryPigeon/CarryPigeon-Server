package team.carrypigeon.backend.chat.domain.config.ws.transport.handlers;

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

import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.config.ws.transport.*;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.logging.LogScope;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAuthState;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeProtocolException;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeAttributes;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeConnection;
import team.carrypigeon.backend.infrastructure.basic.realtime.connection.RealtimeScheduler;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 实时通道处理器。
 * 职责：承接 WebSocket 连接生命周期，并按命令注册表分派上行命令。
 * 边界：认证机制、续传与帧编解码由聚焦协作对象承担；聊天写入只走 HTTP。
 */
@Slf4j
public class NettyRealtimeServiceHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAuthState realtimeAuthState;
    private final RealtimeCommandRegistry commandRegistry;
    private final TimeProvider timeProvider;
    private final int authenticationTimeoutSeconds;

    public NettyRealtimeServiceHandler(
        RealtimeFrameCodec frameCodec,
        RealtimeCommandRegistry commandRegistry,
        RealtimeAuthState realtimeAuthState,
        TimeProvider timeProvider,
        int authenticationTimeoutSeconds
    ) {
        this.frameCodec = frameCodec;
        this.realtimeAuthState = realtimeAuthState;
        this.commandRegistry = commandRegistry;
        this.timeProvider = timeProvider;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
                log.info("Realtime handshake completed, channelId={}, uri={}",
                        context.channel().id().asShortText(), handshakeComplete.requestUri());
                new NettyRealtimeScheduler(context, frameCodec, new NettyRealtimeAttributes(context.channel()), timeProvider).scheduleAuthenticationTimeout(authenticationTimeoutSeconds);
                return;
            }
            if (event instanceof IdleStateEvent) {
                log.info("Realtime connection idle timeout, channelId={}", context.channel().id().asShortText());
                writeFrame(context, new RealtimeFrame.CommandError(
                    null,
                    RealtimeFrameCodec.COMMAND_FRAME_TYPE,
                    "idle_timeout",
                    "realtime connection is idle"
                )).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            super.userEventTriggered(context, event);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        RealtimeClientMessage request = frameCodec.decode(frame.text());
        // 创建上下文
        RealtimeConnection connection = new NettyRealtimeConnection(context);
        RealtimeAttributes attributes = new NettyRealtimeAttributes(context.channel());
        RealtimeScheduler scheduler = new NettyRealtimeScheduler(context, frameCodec, attributes,timeProvider);
        try {
            if (request == null) {
                throw new RealtimeProtocolException("frame_invalid", "frame must be an object");
            }
            if (request.id() != null && !request.id().isBlank()) {
                // 帧级关联：客户端请求 id 优先，缺失时沿用 ContextHandler 生成的帧 id。
                LogContexts.sessionId(request.id());
                LogContexts.traceId(request.id());
            }
            if (request.type() == null || request.type().isBlank()) {
                throw new RealtimeProtocolException("type_blank", "type must not be blank");
            }

            Optional<RealtimeCommandHandler> handler = commandRegistry.find(request.type());
            if (handler.isEmpty()) {
                log.info("Realtime frame rejected, type={}, reason=unsupported_command", request.type());
                writeErrorAndCloseIfUnauthenticated(context,attributes, request, "unsupported_command", "unsupported realtime command");
                return;
            }
            if (handler.get().requiresAuthentication() && attributes.getAccountId() == null) {
                log.info("Realtime frame rejected, type={}, reason=unauthorized", request.type());
                writeErrorAndCloseIfUnauthenticated(context, attributes, request, "unauthorized", "auth must be the first realtime command");
                return;
            }

            handler.get().handle(request, connection, attributes, scheduler, realtimeAuthState);

        } catch (RuntimeException exception) {
            logRealtimeFailure(request, exception);
            writeErrorAndCloseIfUnauthenticated(context, attributes, request, "internal_server_error", "internal server error");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            RealtimeConnection connection = new NettyRealtimeConnection(context);
            RealtimeAttributes attributes = new NettyRealtimeAttributes(context.channel());
            RealtimeScheduler scheduler = new NettyRealtimeScheduler(context, frameCodec, attributes, timeProvider);
            Long accountId = attributes.getAccountId();
            realtimeAuthState.onChannelInactive(connection,attributes,scheduler);
            log.info("Realtime connection closed, channelId={}, uid={}",
                    context.channel().id().asShortText(), accountId);
            super.channelInactive(context);
        }
    }

    /**
     * 记录异常并关闭当前实时连接。
     * 副作用：写入异常日志并主动关闭 Netty 通道，避免异常连接继续留在会话表中。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        try (LogContexts.Scope ignored = LogContexts.openScope(LogScope.WS)) {
            log.warn("Realtime channel exception, channelId={}", context.channel().id().asShortText(), cause);
        }
        context.close();
    }

    /**
     * 按失败语义记录实时帧异常。
     * 约定：协议错误与业务拒绝属于可预期失败，按 info 记录；未预期异常按 warn 保留堆栈。
     *
     * @param request 当前帧请求，可能为空
     * @param exception 待记录的异常
     */
    private void logRealtimeFailure(RealtimeClientMessage request, RuntimeException exception) {
        String requestType = request == null ? null : request.type();
        if (exception instanceof RealtimeProtocolException protocolException) {
            log.info("Realtime frame rejected, type={}, reason={}", requestType, protocolException.reason());
            return;
        }
        if (exception instanceof ProblemException problemException) {
            log.info("Realtime frame rejected, type={}, reason={}", requestType, problemException.reason());
            return;
        }
        log.warn("Realtime frame failed, type={}", requestType, exception);
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
        String reason,
        String message
    ) {
        if (attributes.getAccountId() != null) {
            writeFrame(context, commandError(request, reason, message));
            return;
        }
        writeFrame(context, commandError(request, reason, message)).addListener(ChannelFutureListener.CLOSE);
    }

    private RealtimeFrame.CommandError commandError(RealtimeClientMessage request, String reason, String message) {
        return new RealtimeFrame.CommandError(
            request == null ? null : request.id(),
            request == null ? RealtimeFrameCodec.COMMAND_FRAME_TYPE : request.type(),
            reason,
            message
        );
    }

    private ChannelFuture writeFrame(ChannelHandlerContext context, RealtimeFrame frame) {
        return context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(frame)));
    }
}

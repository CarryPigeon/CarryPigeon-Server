package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeFrame;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * Netty 实时通道处理器。
 * 职责：承接 WebSocket 连接生命周期，并按命令注册表分派上行命令。
 * 边界：认证机制、续传与帧编解码由聚焦协作对象承担；聊天写入只走 HTTP。
 */
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChannelHandler.class);

    /** 无法归属到具体命令时的错误帧类型前缀。 */
    private static final String FRAME_LEVEL_TYPE = "command";

    private final IdGenerator idGenerator;
    private final RealtimeWebSocketDebugLogger debugLogger;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAuthenticationCoordinator authenticationCoordinator;
    private final RealtimeResumeCoordinator resumeCoordinator;
    private final RealtimeCommandRegistry commandRegistry;

    public RealtimeChannelHandler(
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec frameCodec,
            RealtimeCommandRegistry commandRegistry,
            RealtimeResumeCoordinator resumeCoordinator,
            int authenticationTimeoutSeconds,
            boolean requestLogEnabled
    ) {
        this.idGenerator = idGenerator;
        this.frameCodec = frameCodec;
        this.commandRegistry = commandRegistry;
        this.resumeCoordinator = resumeCoordinator;
        this.debugLogger = requestLogEnabled
                ? new RealtimeWebSocketDebugLogger(true)
                : RealtimeWebSocketDebugLogger.disabled();
        this.authenticationCoordinator = new RealtimeAuthenticationCoordinator(
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeConnectionIndex,
                authenticationTimeoutSeconds,
                debugLogger,
                frameCodec
        );
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(idGenerator.nextStringId());
            debugLogger.handshakeComplete(context, handshakeComplete);
            authenticationCoordinator.scheduleAuthenticationTimeout(context);
            return;
        }
        if (event instanceof IdleStateEvent) {
            debugLogger.frameRejected(context, "idle_timeout", null);
            writeFrame(context, new RealtimeFrame.CommandError(
                    null,
                    FRAME_LEVEL_TYPE,
                    "idle_timeout",
                    "realtime connection is idle"
            )).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        RealtimeChannelLogContext.run(context, () -> handleFrame(context, frame));
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        RealtimeChannelLogContext.run(context, () -> authenticationCoordinator.channelInactive(context));
        super.channelInactive(context);
    }

    /**
     * 记录异常并关闭当前实时连接。
     * 副作用：写入异常日志并主动关闭 Netty 通道，避免异常连接继续留在会话表中。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        RealtimeChannelLogContext.run(context, () -> {
            debugLogger.exceptionCaught(context, cause);
            log.warn("Closing realtime channel because of exception", cause);
            context.close();
        });
    }

    /**
     * 创建实时连接读空闲检测处理器。
     * 输出：指定时间无入站数据即触发空闲事件，供本处理器关闭连接。
     */
    public static IdleStateHandler idleStateHandler(int readIdleTimeoutSeconds) {
        if (readIdleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("readIdleTimeoutSeconds must be greater than 0");
        }
        return new IdleStateHandler(readIdleTimeoutSeconds, 0, 0, TimeUnit.SECONDS);
    }

    /**
     * 解析并分派一条客户端命令。
     * 分派顺序：解析信封 → 校验信封 → 查找处理器 → 认证前置 → 交给处理器。
     *
     * @param context Netty 通道上下文
     * @param frame 客户端文本帧
     */
    private void handleFrame(ChannelHandlerContext context, TextWebSocketFrame frame) {
        RealtimeClientMessage request = null;
        try {
            request = frameCodec.decode(frame.text());
            debugLogger.frameReceived(context, request, frame.text().length());
            frameCodec.validate(request);
            Optional<RealtimeCommandHandler> handler = commandRegistry.find(request.type());
            if (handler.isEmpty()) {
                debugLogger.frameRejected(context, "unsupported_command", null);
                writeErrorAndCloseIfUnauthenticated(context, request, "unsupported_command", "unsupported realtime command");
                return;
            }
            if (handler.get().requiresAuthentication() && !authenticationCoordinator.isAuthenticated(context)) {
                debugLogger.frameRejected(context, "unauthorized", null);
                writeErrorAndCloseIfUnauthenticated(context, request, "unauthorized", "auth must be the first realtime command");
                return;
            }
            handler.get().handle(
                    new RealtimeCommandContextImpl(context, request, authenticationCoordinator, resumeCoordinator, frameCodec),
                    request
            );
        } catch (RealtimeFrameCodec.InvalidClientFrameException exception) {
            debugLogger.frameRejected(context, exception.reason(), exception);
            writeErrorAndCloseIfUnauthenticated(context, request, "validation_failed", exception.getMessage());
        } catch (InfrastructureException exception) {
            debugLogger.frameRejected(context, "request_body_invalid", exception);
            writeErrorAndCloseIfUnauthenticated(context, request, "validation_failed", "request body is invalid");
        } catch (ProblemException exception) {
            debugLogger.frameRejected(context, exception.reason(), exception);
            writeErrorAndCloseIfUnauthenticated(context, request, exception.reason(), exception.getMessage());
        } catch (RuntimeException exception) {
            debugLogger.frameRejected(context, "internal_error", exception);
            log.warn("Failed to handle realtime frame", exception);
            writeErrorAndCloseIfUnauthenticated(context, request, "internal_error", "internal server error");
        }
    }

    /**
     * 写出命令错误帧，并在连接未认证时关闭。
     * 约束：未认证连接只有一次出错机会，写完后必须关闭，避免匿名连接长期占用。
     */
    private void writeErrorAndCloseIfUnauthenticated(
            ChannelHandlerContext context,
            RealtimeClientMessage request,
            String reason,
            String message
    ) {
        if (authenticationCoordinator.isAuthenticated(context)) {
            writeFrame(context, commandError(request, reason, message));
            return;
        }
        writeFrame(context, commandError(request, reason, message)).addListener(ChannelFutureListener.CLOSE);
    }

    private RealtimeFrame.CommandError commandError(RealtimeClientMessage request, String reason, String message) {
        return new RealtimeFrame.CommandError(
                request == null ? null : request.id(),
                request == null ? FRAME_LEVEL_TYPE : request.type(),
                reason,
                message
        );
    }

    private ChannelFuture writeFrame(ChannelHandlerContext context, RealtimeFrame frame) {
        return context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(frame)));
    }
}

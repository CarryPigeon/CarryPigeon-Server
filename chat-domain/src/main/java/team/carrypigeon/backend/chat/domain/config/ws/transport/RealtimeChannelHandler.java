package team.carrypigeon.backend.chat.domain.config.ws.transport;

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
import team.carrypigeon.backend.chat.domain.config.ws.downlink.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.config.ws.resume.RealtimeResumeCoordinator;
import team.carrypigeon.backend.chat.domain.config.ws.uplink.RealtimeCommandRegistry;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.realtime.RealtimeProtocolException;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeFrame;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeClientMessage;
import team.carrypigeon.backend.infrastructure.basic.realtime.uplink.RealtimeCommandHandler;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * Netty 实时通道处理器。
 * 职责：承接 WebSocket 连接生命周期，并按命令注册表分派上行命令。
 * 边界：认证机制、续传与帧编解码由聚焦协作对象承担；聊天写入只走 HTTP。
 */
@Slf4j
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final IdGenerator idGenerator;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAuthenticationCoordinator authenticationCoordinator;
    private final RealtimeResumeCoordinator resumeCoordinator;
    private final RealtimeCommandRegistry commandRegistry;

    public RealtimeChannelHandler(
        IdGenerator idGenerator,
        TimeProviderImpl timeProvider,
        RealtimeConnectionIndex realtimeConnectionIndex,
        RealtimeFrameCodec frameCodec,
        RealtimeCommandRegistry commandRegistry,
        RealtimeResumeCoordinator resumeCoordinator,
        AuthTokenApi authTokenApi,
        int authenticationTimeoutSeconds
    ) {
        this.idGenerator = idGenerator;
        this.frameCodec = frameCodec;
        this.commandRegistry = commandRegistry;
        this.resumeCoordinator = resumeCoordinator;
        this.authenticationCoordinator = new RealtimeAuthenticationCoordinator(
            timeProvider,
            realtimeConnectionIndex,
            authTokenApi,
            authenticationTimeoutSeconds,
            frameCodec
        );
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
            String sessionId = idGenerator.nextStringId();
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(sessionId);
            log.debug("Realtime handshake completed, uri={}, sessionId={}",
                handshakeComplete.requestUri(), sessionId);
            authenticationCoordinator.scheduleAuthenticationTimeout(context);
            return;
        }
        if (event instanceof IdleStateEvent) {
            log.debug("Closing idle realtime connection, sessionId={}",
                context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get());
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

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        RealtimeChannelSession.run(context, () -> handleFrame(context, frame));
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        RealtimeChannelSession.run(context, () -> authenticationCoordinator.channelInactive(context));
        super.channelInactive(context);
    }

    /**
     * 记录异常并关闭当前实时连接。
     * 副作用：写入异常日志并主动关闭 Netty 通道，避免异常连接继续留在会话表中。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        RealtimeChannelSession.run(context, () -> {
            log.warn("Closing realtime channel because of exception", cause);
            context.close();
        });
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
     * 解析并分派一条客户端命令。
     * 分派顺序：解析信封 → 校验信封 → 查找处理器 → 认证前置 → 交给处理器。
     *
     * @param context Netty 通道上下文
     * @param frame   客户端文本帧
     */
    private void handleFrame(ChannelHandlerContext context, TextWebSocketFrame frame) {
        RealtimeClientMessage request = null;
        try {
            request = frameCodec.decode(frame.text());
            if (log.isDebugEnabled()) {
                log.debug("Realtime frame received, type={}, id={}, bytes={}",
                    request.type(), request.id(), frame.text().length());
            }
            frameCodec.validate(request);
            Optional<RealtimeCommandHandler> handler = commandRegistry.find(request.type());
            if (handler.isEmpty()) {
                log.debug("Realtime frame rejected, reason=unsupported_command, type={}", request.type());
                writeErrorAndCloseIfUnauthenticated(context, request, "unsupported_command", "unsupported realtime command");
                return;
            }
            if (handler.get().requiresAuthentication() && !authenticationCoordinator.isAuthenticated(context)) {
                log.debug("Realtime frame rejected, reason=unauthorized, type={}", request.type());
                writeErrorAndCloseIfUnauthenticated(context, request, "unauthorized", "auth must be the first realtime command");
                return;
            }
            handler.get().handle(
                new RealtimeCommandScopeImpl(context, request, authenticationCoordinator, resumeCoordinator, frameCodec),
                request
            );
        } catch (RealtimeProtocolException exception) {
            log.debug("Realtime frame rejected, reason={}", exception.reason());
            writeErrorAndCloseIfUnauthenticated(context, request, "validation_failed", exception.getMessage());
        } catch (InfrastructureException exception) {
            log.debug("Realtime frame rejected, reason=request_body_invalid", exception);
            writeErrorAndCloseIfUnauthenticated(context, request, "validation_failed", "request body is invalid");
        } catch (ProblemException exception) {
            log.debug("Realtime frame rejected, reason={}", exception.reason());
            writeErrorAndCloseIfUnauthenticated(context, request, exception.reason(), exception.getMessage());
        } catch (RuntimeException exception) {
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
            request == null ? RealtimeFrameCodec.COMMAND_FRAME_TYPE : request.type(),
            reason,
            message
        );
    }

    private ChannelFuture writeFrame(ChannelHandlerContext context, RealtimeFrame frame) {
        return context.writeAndFlush(new TextWebSocketFrame(frameCodec.encode(frame)));
    }
}

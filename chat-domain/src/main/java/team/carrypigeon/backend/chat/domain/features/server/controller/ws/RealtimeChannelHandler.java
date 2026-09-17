package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * Netty 实时通道处理器。
 * 职责：承接 WebSocket channel 生命周期，并按 v1 客户端命令类型执行顶层分派。
 * 边界：认证计时、事件续传和 frame 映射由聚焦协作对象承担；聊天写入只走 HTTP。
 */
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChannelHandler.class);

    private final IdGenerator idGenerator;
    private final RealtimeWebSocketDebugLogger debugLogger;
    private final RealtimeFrameCodec frameCodec;
    private final RealtimeAuthenticationCoordinator authenticationCoordinator;
    private final RealtimeResumeCoordinator resumeCoordinator;

    public RealtimeChannelHandler(
            JsonProviderImpl jsonProvider,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry
    ) {
        this(
                jsonProvider,
                idGenerator,
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeSessionRegistry,
                10,
                false
        );
    }

    public RealtimeChannelHandler(
            JsonProviderImpl jsonProvider,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            boolean requestLogEnabled
    ) {
        this(
                jsonProvider,
                idGenerator,
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeSessionRegistry,
                10,
                requestLogEnabled
        );
    }

    public RealtimeChannelHandler(
            JsonProviderImpl jsonProvider,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            int authenticationTimeoutSeconds,
            boolean requestLogEnabled
    ) {
        this.idGenerator = idGenerator;
        this.debugLogger = requestLogEnabled
                ? new RealtimeWebSocketDebugLogger(true)
                : RealtimeWebSocketDebugLogger.disabled();
        this.frameCodec = new RealtimeFrameCodec(jsonProvider);
        this.authenticationCoordinator = new RealtimeAuthenticationCoordinator(
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeSessionRegistry,
                authenticationTimeoutSeconds,
                debugLogger,
                frameCodec
        );
        this.resumeCoordinator = new RealtimeResumeCoordinator(realtimeSessionRegistry, frameCodec);
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
            context.writeAndFlush(frameCodec.commandError(
                    null, "command.err", "idle_timeout", "realtime connection is idle"
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

    private void handleFrame(ChannelHandlerContext context, TextWebSocketFrame frame) {
        try {
            RealtimeClientMessage request = frameCodec.parseClientFrame(frame.text());
            debugLogger.frameReceived(context, request, frame.text().length());
            frameCodec.validateClientFrame(request);
            if (!authenticationCoordinator.isAuthenticated(context) && !"auth".equals(request.type())) {
                authenticationCoordinator.rejectUnauthenticatedCommand(context, request);
                return;
            }
            switch (request.type()) {
                case "auth" -> handleAuthentication(context, request, false);
                case "reauth" -> handleAuthentication(context, request, true);
                case "ping" -> context.writeAndFlush(frameCodec.serverFrame("pong", null, null, null));
                default -> context.writeAndFlush(frameCodec.commandError(
                        request.id(),
                        request.type() + ".err",
                        "validation_failed",
                        "unsupported realtime command"
                ));
            }
        } catch (RealtimeFrameCodec.InvalidClientFrameException exception) {
            debugLogger.frameRejected(context, exception.reason(), null);
            authenticationCoordinator.writeErrorAndCloseIfUnauthenticated(
                    context,
                    frameCodec.commandError(null, "command.err", "validation_failed", exception.getMessage())
            );
        } catch (InfrastructureException exception) {
            debugLogger.frameRejected(context, "request_body_invalid", exception);
            authenticationCoordinator.writeErrorAndCloseIfUnauthenticated(
                    context,
                    frameCodec.commandError(null, "command.err", "validation_failed", "request body is invalid")
            );
        } catch (ProblemException exception) {
            String reason = frameCodec.mapReason(exception);
            debugLogger.frameRejected(context, reason, exception);
            context.writeAndFlush(frameCodec.commandError(null, "command.err", reason, exception.getMessage()));
        } catch (RuntimeException exception) {
            debugLogger.frameRejected(context, "internal_error", exception);
            log.warn("Failed to handle realtime frame", exception);
            context.writeAndFlush(frameCodec.commandError(
                    null, "command.err", "internal_error", "internal server error"
            ));
        }
    }

    private void handleAuthentication(
            ChannelHandlerContext context,
            RealtimeClientMessage request,
            boolean reauth
    ) {
        if (!reauth && authenticationCoordinator.isAuthenticated(context)) {
            context.writeAndFlush(frameCodec.commandError(
                    request.id(), "auth.err", "already_authenticated", "use reauth to replace credentials"
            ));
            return;
        }
        authenticationCoordinator.authenticate(
                context,
                request,
                reauth,
                principal -> resumeCoordinator.replay(context, principal.accountId(), request.lastEventId())
        );
    }
}

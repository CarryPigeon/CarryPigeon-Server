package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 文本消息处理器。
 * 职责：承接 v1 WS 首帧 auth/reauth、ping/pong 与事件回放。
 * 边界：聊天写入只走 HTTP，本处理器不接受消息创建命令。
 */
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChannelHandler.class);

    private final JsonProvider jsonProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final AccessTokenAuthenticationApi accessTokenAuthenticationApi;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final int authenticationTimeoutSeconds;
    private final RealtimeWebSocketDebugLogger debugLogger;

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
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
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
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
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            int authenticationTimeoutSeconds,
            boolean requestLogEnabled
    ) {
        if (authenticationTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("authenticationTimeoutSeconds must be greater than 0");
        }
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.accessTokenAuthenticationApi = accessTokenAuthenticationApi;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.authenticationTimeoutSeconds = authenticationTimeoutSeconds;
        this.debugLogger = requestLogEnabled
                ? new RealtimeWebSocketDebugLogger(true)
                : RealtimeWebSocketDebugLogger.disabled();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(idGenerator.nextStringId());
            debugLogger.handshakeComplete(context, (WebSocketServerProtocolHandler.HandshakeComplete) event);
            scheduleAuthenticationTimeout(context);
            return;
        }
        if (event instanceof IdleStateEvent) {
            debugLogger.frameRejected(context, "idle_timeout", null);
            context.writeAndFlush(commandError(null, "command.err", "idle_timeout", "realtime connection is idle"))
                    .addListener(ChannelFutureListener.CLOSE);
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        withMdc(context, () -> {
            try {
                RealtimeClientMessage request = jsonProvider.fromJson(frame.text(), RealtimeClientMessage.class);
                debugLogger.frameReceived(context, request, frame.text().length());
                if (request == null || request.type() == null || request.type().isBlank()) {
                    debugLogger.frameRejected(context, "type_blank", null);
                    writeErrorAndCloseIfUnauthenticated(
                            context,
                            commandError(null, "command.err", "validation_failed", "type must not be blank")
                    );
                    return;
                }
                if (!isAuthenticated(context) && !"auth".equals(request.type())) {
                    rejectUnauthenticatedCommand(context, request);
                    return;
                }
                switch (request.type()) {
                    case "auth" -> {
                        if (isAuthenticated(context)) {
                            context.writeAndFlush(commandError(
                                    request.id(), "auth.err", "already_authenticated", "use reauth to replace credentials"
                            ));
                        } else {
                            handleAuth(context, request, false);
                        }
                    }
                    case "reauth" -> handleAuth(context, request, true);
                    case "ping" -> context.writeAndFlush(serverFrame("pong", null, null, null));
                    default -> context.writeAndFlush(commandError(
                            request.id(), request.type() + ".err", "validation_failed", "unsupported realtime command"
                    ));
                }
            } catch (InfrastructureException exception) {
                debugLogger.frameRejected(context, "request_body_invalid", exception);
                writeErrorAndCloseIfUnauthenticated(
                        context,
                        commandError(null, "command.err", "validation_failed", "request body is invalid")
                );
            } catch (ProblemException exception) {
                debugLogger.frameRejected(context, mapReason(exception), exception);
                context.writeAndFlush(commandError(null, "command.err", mapReason(exception), exception.getMessage()));
            } catch (RuntimeException exception) {
                debugLogger.frameRejected(context, "internal_error", exception);
                log.warn("Failed to handle realtime frame", exception);
                context.writeAndFlush(commandError(null, "command.err", "internal_error", "internal server error"));
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        withMdc(context, () -> {
            cancelAuthenticationTimeout(context);
            cancelAccessTokenExpiration(context);
            debugLogger.channelInactive(context);
            AuthenticatedAccount principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
            if (principal != null) {
                realtimeSessionRegistry.unregister(principal.accountId(), context.channel());
            }
        });
        super.channelInactive(context);
    }

    /**
     * 记录异常并关闭当前实时连接。
     * 副作用：写入异常日志并主动关闭 Netty 通道，避免异常连接继续留在会话表中。
     *
     * @param context Netty 通道上下文
     * @param cause 当前异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        withMdc(context, () -> {
            debugLogger.exceptionCaught(context, cause);
            log.warn("Closing realtime channel because of exception", cause);
            context.close();
        });
    }

    /**
     * 创建实时连接读空闲检测处理器。
     * 输出：指定时间无入站数据即触发空闲事件，供本处理器关闭连接。
     *
     * @param readIdleTimeoutSeconds 读空闲超时秒数
     * @return Netty 读空闲处理器
     */
    public static IdleStateHandler idleStateHandler(int readIdleTimeoutSeconds) {
        if (readIdleTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("readIdleTimeoutSeconds must be greater than 0");
        }
        return new IdleStateHandler(readIdleTimeoutSeconds, 0, 0, TimeUnit.SECONDS);
    }

    /**
     * 处理 realtime 认证或重新认证命令。
     * 副作用：校验 access token、更新通道 principal、维护会话注册表，并在认证成功后尝试补发离线事件。
     *
     * @param context Netty 通道上下文
     * @param request 客户端认证命令
     * @param reauth true 表示已有连接上的重新认证
     */
    private void handleAuth(ChannelHandlerContext context, RealtimeClientMessage request, boolean reauth) {
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            debugLogger.authResult(context, request, reauth, false, "unauthorized");
            context.writeAndFlush(commandError(request.id(), reauth ? "reauth.err" : "auth.err", "unauthorized", "authentication is required"));
            return;
        }
        try {
            AccessTokenAuthenticationResult authentication = accessTokenAuthenticationApi.authenticate(accessToken);
            AuthenticatedAccount principal = new AuthenticatedAccount(
                    authentication.accountId(),
                    authentication.username()
            );
            AuthenticatedAccount previousPrincipal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
            if (previousPrincipal != null && previousPrincipal.accountId() != principal.accountId()) {
                realtimeSessionRegistry.unregister(previousPrincipal.accountId(), context.channel());
            }
            context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(principal);
            realtimeSessionRegistry.register(principal.accountId(), context.channel());
            cancelAuthenticationTimeout(context);
            scheduleAccessTokenExpiration(context, authentication.expiresAt());
            debugLogger.authResult(context, request, reauth, true, "");
            context.writeAndFlush(serverFrame(
                    reauth ? "reauth.ok" : "auth.ok",
                    request.id(),
                    Map.of(
                            "uid", Long.toString(authentication.accountId()),
                            "expires_at", authentication.expiresAt().toEpochMilli(),
                            "server_id", serverIdentityProperties.id()
                    ),
                    null
            ));
            replayEvents(context, principal.accountId(), request.lastEventId());
        } catch (ProblemException exception) {
            debugLogger.authResult(context, request, reauth, false, mapReason(exception));
            context.writeAndFlush(commandError(request.id(), reauth ? "reauth.err" : "auth.err", mapReason(exception), exception.getMessage()));
        }
    }

    /**
     * 在 WebSocket 握手完成后启动首帧鉴权超时计时。
     * 失败语义：超时时仍无 principal 则下发 auth.err 并关闭通道。
     *
     * @param context Netty 通道上下文
     */
    private void scheduleAuthenticationTimeout(ChannelHandlerContext context) {
        cancelAuthenticationTimeout(context);
        ScheduledFuture<?> timeoutFuture = context.executor().schedule(() -> withMdc(context, () -> {
            if (context.channel().isActive() && !isAuthenticated(context)) {
                debugLogger.frameRejected(context, "authentication_timeout", null);
                context.writeAndFlush(commandError(
                        null, "auth.err", "authentication_timeout", "authentication frame was not received in time"
                )).addListener(ChannelFutureListener.CLOSE);
            }
        }), authenticationTimeoutSeconds, TimeUnit.SECONDS);
        context.channel().attr(RealtimeChannelSession.AUTHENTICATION_TIMEOUT_FUTURE_KEY).set(timeoutFuture);
    }

    /**
     * 取消当前连接尚未执行的首帧鉴权超时任务。
     *
     * @param context Netty 通道上下文
     */
    private void cancelAuthenticationTimeout(ChannelHandlerContext context) {
        ScheduledFuture<?> timeoutFuture = context.channel()
                .attr(RealtimeChannelSession.AUTHENTICATION_TIMEOUT_FUTURE_KEY)
                .getAndSet(null);
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    /**
     * 按 access token 的绝对过期时间关闭实时连接。
     * 约束：reauth 会替换旧任务；旧任务不得关闭已换新 token 的会话。
     */
    private void scheduleAccessTokenExpiration(ChannelHandlerContext context, Instant expiresAt) {
        cancelAccessTokenExpiration(context);
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY).set(expiresAt);
        long delayMillis = Math.max(0L, Duration.between(timeProvider.nowInstant(), expiresAt).toMillis());
        ScheduledFuture<?> expirationFuture = context.executor().schedule(() -> withMdc(context, () -> {
            Instant currentExpiresAt = context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY).get();
            if (context.channel().isActive()
                    && isAuthenticated(context)
                    && expiresAt.equals(currentExpiresAt)) {
                debugLogger.frameRejected(context, "token_expired", null);
                context.writeAndFlush(commandError(
                        null, "auth.err", "token_expired", "access token is expired"
                )).addListener(ChannelFutureListener.CLOSE);
            }
        }), delayMillis, TimeUnit.MILLISECONDS);
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRATION_FUTURE_KEY).set(expirationFuture);
    }

    private void cancelAccessTokenExpiration(ChannelHandlerContext context) {
        ScheduledFuture<?> expirationFuture = context.channel()
                .attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRATION_FUTURE_KEY)
                .getAndSet(null);
        if (expirationFuture != null) {
            expirationFuture.cancel(false);
        }
        context.channel().attr(RealtimeChannelSession.ACCESS_TOKEN_EXPIRES_AT_KEY).set(null);
    }

    private boolean isAuthenticated(ChannelHandlerContext context) {
        return context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get() != null;
    }

    private void rejectUnauthenticatedCommand(ChannelHandlerContext context, RealtimeClientMessage request) {
        debugLogger.frameRejected(context, "unauthorized", null);
        context.writeAndFlush(commandError(
                request.id(), "auth.err", "unauthorized", "auth must be the first realtime command"
        )).addListener(ChannelFutureListener.CLOSE);
    }

    private void writeErrorAndCloseIfUnauthenticated(ChannelHandlerContext context, TextWebSocketFrame errorFrame) {
        if (isAuthenticated(context)) {
            context.writeAndFlush(errorFrame);
        } else {
            context.writeAndFlush(errorFrame).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 根据客户端上报的 lastEventId 补发实时事件。
     * 失败语义：事件缓存已无法覆盖该位置时下发 `resume.failed`，由客户端重新拉取状态。
     *
     * @param context Netty 通道上下文
     * @param accountId 当前账号 ID
     * @param lastEventId 客户端最后确认的事件 ID
     */
    private void replayEvents(ChannelHandlerContext context, long accountId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }
        List<RealtimeSessionRegistry.StoredRealtimeEvent> events = realtimeSessionRegistry.eventsAfter(accountId, lastEventId);
        if (events == null) {
            context.writeAndFlush(serverFrame("resume.failed", null, Map.of("reason", "event_too_old"), null));
            return;
        }
        for (RealtimeSessionRegistry.StoredRealtimeEvent event : events) {
            context.writeAndFlush(eventFrame(event));
        }
    }

    /**
     * 把领域问题映射为 WebSocket v1 错误 reason。
     * 约束：认证问题统一为 unauthorized，权限不足统一为 forbidden，内部问题统一隐藏为 internal_error。
     *
     * @param exception 领域问题异常
     * @return WebSocket 错误 reason
     */
    private String mapReason(ProblemException exception) {
        return switch (exception.type()) {
            case FORBIDDEN -> switch (exception.reason()) {
                case "authentication_required", "invalid_access_token", "invalid_refresh_token", "invalid_token" -> "unauthorized";
                case "invalid_credentials",
                     "private_channel_required",
                     "channel_invite_forbidden",
                     "channel_profile_forbidden",
                     "channel_role_forbidden",
                     "channel_ownership_forbidden",
                     "channel_ban_forbidden",
            "channel_pin_forbidden",
                     "channel_membership_required",
                     "not_channel_member",
                     "channel_message_recall_forbidden",
                     "system_channel_membership_required",
                     "system_channel_members_hidden",
                     "system_channel_required" -> "forbidden";
                default -> exception.reason();
            };
            case VALIDATION -> switch (exception.reason()) {
                case "application_already_processed" -> "conflict";
                default -> exception.reason();
            };
            case INTERNAL -> "internal_error";
            default -> exception.reason();
        };
    }

    /**
     * 构造命令错误响应帧。
     * 输出：遵循 v1 envelope 的错误 frame，携带原请求 ID、错误类型和稳定 reason。
     *
     * @param id 客户端请求 ID
     * @param type 错误帧类型
     * @param reason 稳定错误 reason
     * @param message 客户端可读错误消息
     * @return WebSocket 文本帧
     */
    private TextWebSocketFrame commandError(String id, String type, String reason, String message) {
        return serverFrame(type, id, null, Map.of("reason", reason, "message", message));
    }

    /**
     * 把缓存事件转换为 v1 event frame。
     * 用途：连接恢复时向客户端补发 missed events。
     *
     * @param event 已缓存实时事件
     * @return WebSocket 文本帧
     */
    private TextWebSocketFrame eventFrame(RealtimeSessionRegistry.StoredRealtimeEvent event) {
        return serverFrame("event", null, Map.of(
                "event_id", event.eventId(),
                "event_type", event.eventType(),
                "server_time", event.serverTime(),
                "payload", event.payload()
        ), null);
    }

    /**
     * 构造统一服务端 WebSocket 文本帧。
     * 约束：所有服务端下行消息都使用 `RealtimeServerMessage` envelope 序列化。
     *
     * @param type 下行帧类型
     * @param id 客户端请求 ID，可为空
     * @param data 成功载荷，可为空
     * @param error 错误载荷，可为空
     * @return WebSocket 文本帧
     */
    private TextWebSocketFrame serverFrame(String type, String id, Object data, Object error) {
        return new TextWebSocketFrame(jsonProvider.toJson(new RealtimeServerMessage(type, id, data, error)));
    }

    /**
     * 在当前通道的日志上下文中执行动作。
     * 副作用：临时写入 traceId、requestId、route 和 uid 到 MDC，执行结束后清理。
     *
     * @param context Netty 通道上下文
     * @param action 需要在 MDC 上下文中执行的动作
     */
    private void withMdc(ChannelHandlerContext context, Runnable action) {
        AuthenticatedAccount principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        try {
            LogContexts.traceId(context.channel().attr(RealtimeChannelSession.TRACE_ID_KEY).get());
            LogContexts.requestId(context.channel().attr(RealtimeChannelSession.REQUEST_ID_KEY).get());
            LogContexts.route(context.channel().attr(RealtimeChannelSession.ROUTE_KEY).get());
            if (principal != null) {
                LogContexts.uid(Long.toString(principal.accountId()));
            }
            action.run();
        } finally {
            LogContexts.clear();
        }
    }
}

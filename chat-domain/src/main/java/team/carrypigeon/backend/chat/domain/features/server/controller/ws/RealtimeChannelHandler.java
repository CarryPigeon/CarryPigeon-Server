package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 文本消息处理器。
 * 职责：承接 v1 WS 首帧 auth/reauth、ping/pong、事件回放与入站命令分发。
 * 边界：只负责协议解析与结果回写，不在处理器内承载消息业务规则。
 */
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChannelHandler.class);

    private final JsonProvider jsonProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final AuthTokenService authTokenService;
    private final ServerIdentityProperties serverIdentityProperties;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final Supplier<ChannelMessagePublishingApi> channelMessagePublishingApiSupplier;
    private final Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier;
    private final RealtimeWebSocketDebugLogger debugLogger;

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            Supplier<ChannelMessagePublishingApi> channelMessagePublishingApiSupplier,
            Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier
    ) {
        this(
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                serverIdentityProperties,
                realtimeSessionRegistry,
                channelMessagePublishingApiSupplier,
                realtimeInboundMessageDispatcherSupplier,
                false
        );
    }

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            Supplier<ChannelMessagePublishingApi> channelMessagePublishingApiSupplier,
            Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier,
            boolean requestLogEnabled
    ) {
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.authTokenService = authTokenService;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.channelMessagePublishingApiSupplier = channelMessagePublishingApiSupplier;
        this.realtimeInboundMessageDispatcherSupplier = realtimeInboundMessageDispatcherSupplier;
        this.debugLogger = requestLogEnabled
                ? new RealtimeWebSocketDebugLogger(true)
                : RealtimeWebSocketDebugLogger.disabled();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(idGenerator.nextStringId());
            debugLogger.handshakeComplete(context, (WebSocketServerProtocolHandler.HandshakeComplete) event);
            return;
        }
        if (event instanceof IdleStateEvent) {
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
                    context.writeAndFlush(commandError(null, "command.err", "validation_failed", "type must not be blank"));
                    return;
                }
                switch (request.type()) {
                    case "auth" -> handleAuth(context, request, false);
                    case "reauth" -> handleAuth(context, request, true);
                    case "ping" -> context.writeAndFlush(serverFrame("pong", null, null, null));
                    default -> handleCommand(context, request);
                }
            } catch (InfrastructureException exception) {
                debugLogger.frameRejected(context, "request_body_invalid", exception);
                context.writeAndFlush(commandError(null, "command.err", "validation_failed", "request body is invalid"));
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
     * 输出：60 秒无入站数据即触发空闲事件，供上层决定是否关闭连接。
     *
     * @return Netty 读空闲处理器
     */
    public static IdleStateHandler idleStateHandler() {
        return new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS);
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
            AuthTokenClaims claims = authTokenService.parseAccessToken(accessToken);
            AuthenticatedAccount principal = new AuthenticatedAccount(parseAccessSubjectAccountId(claims), claims.username());
            AuthenticatedAccount previousPrincipal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
            if (previousPrincipal != null && previousPrincipal.accountId() != principal.accountId()) {
                realtimeSessionRegistry.unregister(previousPrincipal.accountId(), context.channel());
            }
            context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(principal);
            realtimeSessionRegistry.register(principal.accountId(), context.channel());
            debugLogger.authResult(context, request, reauth, true, "");
            context.writeAndFlush(serverFrame(
                    reauth ? "reauth.ok" : "auth.ok",
                    request.id(),
                    Map.of(
                            "uid", claims.subject(),
                            "expires_at", claims.expiresAt().toEpochMilli(),
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
     * 从 access token claims 中解析实时连接账号 ID。
     * 失败语义：token subject 不是正数账号 ID 时返回非法 access token，供 WS auth 映射为 unauthorized。
     *
     * @param claims 已校验的 access token claims
     * @return 当前实时连接账号 ID
     */
    private long parseAccessSubjectAccountId(AuthTokenClaims claims) {
        try {
            long accountId = Long.parseLong(claims.subject());
            if (accountId <= 0L) {
                throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
            }
            return accountId;
        } catch (NumberFormatException exception) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
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
     * 分发已认证连接上的业务命令。
     * 约束：命令必须在认证后执行，且只通过 realtime 入站分发器调用领域 API。
     *
     * @param context Netty 通道上下文
     * @param request 客户端业务命令
     */
    private void handleCommand(ChannelHandlerContext context, RealtimeClientMessage request) {
        AuthenticatedAccount principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        if (principal == null) {
            debugLogger.frameRejected(context, "unauthorized", null);
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", "unauthorized", "authentication is required"));
            return;
        }
        ChannelMessagePublishingApi channelMessagePublishingApi = channelMessagePublishingApiSupplier.get();
        RealtimeInboundMessageDispatcher dispatcher = realtimeInboundMessageDispatcherSupplier.get();
        if (channelMessagePublishingApi == null || dispatcher == null) {
            debugLogger.frameRejected(context, "realtime_message_service_unavailable", null);
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", "internal_error", "realtime message service is unavailable"));
            return;
        }
        try {
            dispatcher.dispatch(principal, request, channelMessagePublishingApi);
        } catch (ProblemException exception) {
            debugLogger.frameRejected(context, mapReason(exception), exception);
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", mapReason(exception), exception.getMessage()));
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
                     "system_channel_required",
                     "message_not_editable",
                     "message_edit_window_expired" -> "forbidden";
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

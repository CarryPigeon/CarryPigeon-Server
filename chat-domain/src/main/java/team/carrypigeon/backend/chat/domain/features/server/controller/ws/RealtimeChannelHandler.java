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
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
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
    private final Supplier<MessageApplicationService> messageApplicationServiceSupplier;
    private final Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier;

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            Supplier<MessageApplicationService> messageApplicationServiceSupplier,
            Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier
    ) {
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.authTokenService = authTokenService;
        this.serverIdentityProperties = serverIdentityProperties;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.messageApplicationServiceSupplier = messageApplicationServiceSupplier;
        this.realtimeInboundMessageDispatcherSupplier = realtimeInboundMessageDispatcherSupplier;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(idGenerator.nextStringId());
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
                if (request == null || request.type() == null || request.type().isBlank()) {
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
                context.writeAndFlush(commandError(null, "command.err", "validation_failed", "request body is invalid"));
            } catch (RuntimeException exception) {
                log.warn("Failed to handle realtime frame", exception);
                context.writeAndFlush(commandError(null, "command.err", "internal_error", "internal server error"));
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        withMdc(context, () -> {
            AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
            if (principal != null) {
                realtimeSessionRegistry.unregister(principal.accountId(), context.channel());
            }
        });
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        withMdc(context, () -> {
            log.warn("Closing realtime channel because of exception", cause);
            context.close();
        });
    }

    public static IdleStateHandler idleStateHandler() {
        return new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS);
    }

    private void handleAuth(ChannelHandlerContext context, RealtimeClientMessage request, boolean reauth) {
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            context.writeAndFlush(commandError(request.id(), reauth ? "reauth.err" : "auth.err", "unauthorized", "authentication is required"));
            return;
        }
        try {
            AuthTokenClaims claims = authTokenService.parseAccessToken(accessToken);
            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(Long.parseLong(claims.subject()), claims.username());
            context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(principal);
            realtimeSessionRegistry.register(principal.accountId(), context.channel());
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
            replayEvents(context, request.lastEventId());
        } catch (ProblemException exception) {
            context.writeAndFlush(commandError(request.id(), reauth ? "reauth.err" : "auth.err", mapReason(exception), exception.getMessage()));
        }
    }

    private void replayEvents(ChannelHandlerContext context, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return;
        }
        List<RealtimeSessionRegistry.StoredRealtimeEvent> events = realtimeSessionRegistry.eventsAfter(lastEventId);
        if (events == null) {
            context.writeAndFlush(serverFrame("resume.failed", null, Map.of("reason", "event_too_old"), null));
            return;
        }
        for (RealtimeSessionRegistry.StoredRealtimeEvent event : events) {
            context.writeAndFlush(eventFrame(event));
        }
    }

    private void handleCommand(ChannelHandlerContext context, RealtimeClientMessage request) {
        AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        if (principal == null) {
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", "unauthorized", "authentication is required"));
            return;
        }
        MessageApplicationService messageApplicationService = messageApplicationServiceSupplier.get();
        RealtimeInboundMessageDispatcher dispatcher = realtimeInboundMessageDispatcherSupplier.get();
        if (messageApplicationService == null || dispatcher == null) {
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", "internal_error", "realtime message service is unavailable"));
            return;
        }
        try {
            dispatcher.dispatch(principal, request, messageApplicationService);
        } catch (ProblemException exception) {
            context.writeAndFlush(commandError(request.id(), request.type() + ".err", mapReason(exception), exception.getMessage()));
        }
    }

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

    private TextWebSocketFrame commandError(String id, String type, String reason, String message) {
        return serverFrame(type, id, null, Map.of("reason", reason, "message", message));
    }

    private TextWebSocketFrame eventFrame(RealtimeSessionRegistry.StoredRealtimeEvent event) {
        return serverFrame("event", null, Map.of(
                "event_id", event.eventId(),
                "event_type", event.eventType(),
                "server_time", event.serverTime(),
                "payload", event.payload()
        ), null);
    }

    private TextWebSocketFrame serverFrame(String type, String id, Object data, Object error) {
        return new TextWebSocketFrame(jsonProvider.toJson(new RealtimeServerMessage(type, id, data, error)));
    }

    private void withMdc(ChannelHandlerContext context, Runnable action) {
        AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
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

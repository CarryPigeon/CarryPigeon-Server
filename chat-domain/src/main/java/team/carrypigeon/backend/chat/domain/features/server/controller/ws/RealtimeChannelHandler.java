package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 文本消息处理器。
 * 职责：在 WebSocket 会话建立后处理欢迎消息、心跳和最小业务命令分发。
 * 边界：只负责协议解析与结果回写，不在处理器内承载消息业务规则。
 */
@Slf4j
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final JsonProvider jsonProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final RealtimeSessionRegistry realtimeSessionRegistry;
    private final Supplier<MessageApplicationService> messageApplicationServiceSupplier;
    private final Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier;

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            RealtimeSessionRegistry realtimeSessionRegistry,
            Supplier<MessageApplicationService> messageApplicationServiceSupplier,
            Supplier<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherSupplier
    ) {
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.realtimeSessionRegistry = realtimeSessionRegistry;
        this.messageApplicationServiceSupplier = messageApplicationServiceSupplier;
        this.realtimeInboundMessageDispatcherSupplier = realtimeInboundMessageDispatcherSupplier;
    }

    public RealtimeChannelHandler(
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            RealtimeSessionRegistry realtimeSessionRegistry,
            Supplier<MessageApplicationService> messageApplicationServiceSupplier
    ) {
        this(jsonProvider, idGenerator, timeProvider, realtimeSessionRegistry, messageApplicationServiceSupplier, () -> null);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
            if (principal == null) {
                log.warn("Closing realtime channel because authenticated principal is missing after handshake");
                context.close();
                return;
            }
            String sessionId = idGenerator.nextStringId();
            context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).set(sessionId);
            realtimeSessionRegistry.register(principal.accountId(), context.channel());
            log.info("Realtime channel connected for account {} with session {}", principal.accountId(), sessionId);
            context.writeAndFlush(toFrame("welcome", sessionId, new RealtimeNoticePayload("realtime channel connected")));
            return;
        }
        if (event instanceof IdleStateEvent) {
            String sessionId = context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get();
            context.writeAndFlush(toFrame("heartbeat", sessionId, new RealtimeNoticePayload("pong")));
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        String sessionId = context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get();
        AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        if (principal == null) {
            context.writeAndFlush(problemFrame(sessionId, 300, "authentication is required"));
            return;
        }
        try {
            RealtimeClientMessage request = jsonProvider.fromJson(frame.text(), RealtimeClientMessage.class);
            if (request == null || request.type() == null || request.type().isBlank()) {
                context.writeAndFlush(problemFrame(sessionId, 200, "type must not be blank"));
                return;
            }
            MessageApplicationService messageApplicationService = messageApplicationServiceSupplier.get();
            if (messageApplicationService == null) {
                context.writeAndFlush(problemFrame(sessionId, 500, "realtime message service is unavailable"));
                return;
            }
            RealtimeInboundMessageDispatcher dispatcher = realtimeInboundMessageDispatcherSupplier.get();
            if (dispatcher == null) {
                context.writeAndFlush(problemFrame(sessionId, 500, "realtime message dispatcher is unavailable"));
                return;
            }
            dispatcher.dispatch(principal, request, messageApplicationService);
        } catch (ProblemException exception) {
            context.writeAndFlush(problemFrame(sessionId, problemCode(exception), exception.getMessage()));
        } catch (InfrastructureException exception) {
            context.writeAndFlush(problemFrame(sessionId, 200, "request body is invalid"));
        } catch (RuntimeException exception) {
            log.warn("Failed to handle realtime message for session {}", sessionId, exception);
            context.writeAndFlush(problemFrame(sessionId, 500, "internal server error"));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        AuthenticatedPrincipal principal = context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        if (principal != null) {
            realtimeSessionRegistry.unregister(principal.accountId(), context.channel());
        }
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.warn("Closing realtime channel because of exception", cause);
        context.close();
    }

    /**
     * 创建默认的空闲检测处理器。
     *
     * @return 用于心跳检测的空闲处理器
     */
    public static IdleStateHandler idleStateHandler() {
        return new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS);
    }

    private TextWebSocketFrame toFrame(String type, String sessionId, Object data) {
        RealtimeServerMessage message = new RealtimeServerMessage(
                type,
                sessionId,
                timeProvider.nowMillis(),
                data
        );
        return new TextWebSocketFrame(jsonProvider.toJson(message));
    }

    private TextWebSocketFrame problemFrame(String sessionId, int code, String message) {
        return toFrame("problem", sessionId, new RealtimeProblemPayload(code, message));
    }

    private int problemCode(ProblemException exception) {
        return switch (exception.type()) {
            case VALIDATION -> 200;
            case FORBIDDEN -> 300;
            case NOT_FOUND -> 404;
            case INTERNAL -> 500;
        };
    }

    private record RealtimeNoticePayload(String message) {
    }

    private record RealtimeProblemPayload(int code, String message) {
    }
}

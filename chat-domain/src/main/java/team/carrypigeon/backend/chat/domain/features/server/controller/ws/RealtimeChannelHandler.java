package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * Netty 文本消息处理器。
 * 职责：在 WebSocket 会话建立后处理欢迎消息、心跳和文本回显。
 * 边界：当前阶段只提供实时通道骨架，不承载具体聊天命令分发。
 */
@Slf4j
public class RealtimeChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final JsonProvider jsonProvider;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;

    public RealtimeChannelHandler(JsonProvider jsonProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.jsonProvider = jsonProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
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
            log.info("Realtime channel connected for account {} with session {}", principal.accountId(), sessionId);
            context.writeAndFlush(toFrame("welcome", sessionId, "realtime channel connected"));
            return;
        }
        if (event instanceof IdleStateEvent) {
            String sessionId = context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get();
            context.writeAndFlush(toFrame("heartbeat", sessionId, "pong"));
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
        String payload = frame.text();
        String sessionId = context.channel().attr(RealtimeChannelSession.SESSION_ID_KEY).get();
        log.debug("Received realtime text frame from session {}", sessionId);
        context.writeAndFlush(toFrame("echo", sessionId, payload));
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

    private TextWebSocketFrame toFrame(String type, String sessionId, String content) {
        RealtimeServerMessage message = new RealtimeServerMessage(
                type,
                sessionId,
                timeProvider.nowMillis(),
                content
        );
        return new TextWebSocketFrame(jsonProvider.toJson(message));
    }
}

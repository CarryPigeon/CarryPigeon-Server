package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RealtimeChannelHandler 生命周期契约测试。
 * 职责：验证握手完成后的欢迎消息与会话绑定行为。
 * 边界：不验证业务消息分发和错误回写，只验证生命周期入口行为。
 */
@Tag("contract")
class RealtimeChannelHandlerLifecycleTests {

    /**
     * 验证握手完成后会回写欢迎消息并绑定会话。
     */
    @Test
    @DisplayName("user event handshake complete sends welcome message")
    void userEvent_handshakeComplete_sendsWelcomeMessage() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));
        sender.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).set(new AuthenticatedPrincipal(1001L, "carry-user"));
        sender.attr(RealtimeChannelSession.REQUEST_ID_KEY).set("req-1");
        sender.attr(RealtimeChannelSession.TRACE_ID_KEY).set("trace-1");
        sender.attr(RealtimeChannelSession.ROUTE_KEY).set("/ws");

        sender.pipeline().fireUserEventTriggered(new WebSocketServerProtocolHandler.HandshakeComplete("/ws", null, null));

        TextWebSocketFrame frame = sender.readOutbound();
        assertNotNull(frame);
        assertTrue(frame.text().contains("\"type\":\"welcome\""));
        assertEquals(1, registry.getChannels(1001L).size());
        assertNull(MDC.get(LogKeys.REQUEST_ID));
        assertNull(MDC.get(LogKeys.TRACE_ID));
        assertNull(MDC.get(LogKeys.ROUTE));
        assertNull(MDC.get(LogKeys.UID));
    }

    /**
     * 验证未完成握手前不会发送心跳消息。
     */
    @Test
    @DisplayName("user event idle state before handshake does not send heartbeat")
    void userEvent_idleStateBeforeHandshake_doesNotSendHeartbeat() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel sender = RealtimeChannelHandlerTestSupport.channel(registry, RealtimeChannelHandlerTestSupport.service(registry));

        sender.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);

        assertFalse(sender.outboundMessages().iterator().hasNext());
        assertNull(MDC.get(LogKeys.REQUEST_ID));
        assertNull(MDC.get(LogKeys.TRACE_ID));
        assertNull(MDC.get(LogKeys.ROUTE));
        assertNull(MDC.get(LogKeys.UID));
    }
}

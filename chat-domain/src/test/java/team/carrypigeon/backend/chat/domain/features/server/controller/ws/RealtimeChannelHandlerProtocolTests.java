package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `RealtimeChannelHandler` 协议分派契约测试。
 * 职责：验证 frame 编解码与 resume 协作对象拆分后仍保持下行顺序、失败 reason 和关闭行为。
 * 边界：不验证 HTTP 消息写入、领域事件生产或真实网络监听。
 */
@Tag("contract")
class RealtimeChannelHandlerProtocolTests {

    /**
     * 验证认证成功帧先于最后确认事件之后的 replay 事件下发。
     */
    @Test
    @DisplayName("auth frame valid resume anchor replays later events")
    void channelRead_authFrameWithValidResumeAnchor_replaysLaterEvents() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        registry.appendEvent(RealtimeSessionRegistry.event(
                "9001", "message.created", 1000L, Map.of("mid", "5001"), List.of(1001L)
        ));
        registry.appendEvent(RealtimeSessionRegistry.event(
                "9002", "message.recalled", 1001L, Map.of("mid", "5002"), List.of(1001L)
        ));
        EmbeddedChannel channel = authenticatedChannel(registry, "9001");

        TextWebSocketFrame authFrame = channel.readOutbound();
        TextWebSocketFrame replayFrame = channel.readOutbound();

        assertTrue(authFrame.text().contains("\"type\":\"auth.ok\""));
        assertTrue(replayFrame.text().contains("\"type\":\"event\""));
        assertTrue(replayFrame.text().contains("\"event_id\":\"9002\""));
        assertTrue(replayFrame.text().contains("\"event_type\":\"message.recalled\""));
        assertNull(channel.readOutbound());
    }

    /**
     * 验证当前账户窗口找不到客户端确认锚点时返回稳定的 resume.failed。
     */
    @Test
    @DisplayName("auth frame missing resume anchor replies resume failed")
    void channelRead_authFrameWithMissingResumeAnchor_repliesResumeFailed() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = authenticatedChannel(registry, "8999");

        TextWebSocketFrame authFrame = channel.readOutbound();
        TextWebSocketFrame resumeFrame = channel.readOutbound();

        assertTrue(authFrame.text().contains("\"type\":\"auth.ok\""));
        assertTrue(resumeFrame.text().contains("\"type\":\"resume.failed\""));
        assertTrue(resumeFrame.text().contains("\"reason\":\"event_too_old\""));
        assertTrue(channel.isActive());
    }

    /**
     * 验证未认证连接发送非法 JSON 时返回原有校验错误并关闭连接。
     */
    @Test
    @DisplayName("malformed json unauthenticated replies validation error and closes")
    void channelRead_malformedJsonWhileUnauthenticated_repliesValidationErrorAndCloses() {
        EmbeddedChannel channel = openChannel(new RealtimeSessionRegistry());

        channel.writeInbound(new TextWebSocketFrame("{"));

        TextWebSocketFrame errorFrame = channel.readOutbound();
        assertTrue(errorFrame.text().contains("\"type\":\"command.err\""));
        assertTrue(errorFrame.text().contains("\"reason\":\"validation_failed\""));
        assertFalse(channel.isActive());
    }

    /**
     * 验证未认证连接发送空 type 时保持 validation_failed 与主动关闭语义。
     */
    @Test
    @DisplayName("blank type unauthenticated replies validation error and closes")
    void channelRead_blankTypeWhileUnauthenticated_repliesValidationErrorAndCloses() {
        EmbeddedChannel channel = openChannel(new RealtimeSessionRegistry());

        channel.writeInbound(new TextWebSocketFrame("{\"type\":\"\",\"id\":\"1\"}"));

        TextWebSocketFrame errorFrame = channel.readOutbound();
        assertTrue(errorFrame.text().contains("\"type\":\"command.err\""));
        assertTrue(errorFrame.text().contains("\"reason\":\"validation_failed\""));
        assertFalse(channel.isActive());
    }

    private EmbeddedChannel authenticatedChannel(RealtimeSessionRegistry registry, String lastEventId) {
        EmbeddedChannel channel = openChannel(registry);
        channel.writeInbound(new TextWebSocketFrame("""
                {"type":"auth","id":"1","data":{"access_token":"access-token","resume":{"last_event_id":"%s"}}}
                """.formatted(lastEventId)));
        return channel;
    }

    private EmbeddedChannel openChannel(RealtimeSessionRegistry registry) {
        EmbeddedChannel channel = RealtimeChannelHandlerTestSupport.channel(registry);
        channel.pipeline().fireUserEventTriggered(
                new WebSocketServerProtocolHandler.HandshakeComplete("/api/ws", null, null)
        );
        return channel;
    }
}

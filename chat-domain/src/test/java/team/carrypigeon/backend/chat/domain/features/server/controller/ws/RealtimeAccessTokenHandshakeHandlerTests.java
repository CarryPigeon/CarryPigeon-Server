package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * RealtimeAccessTokenHandshakeHandler 契约测试。
 * 职责：验证 WS 升级前只准备请求链路上下文，不再要求握手 Bearer 鉴权。
 * 边界：不验证 Netty 协议升级细节，只验证握手前处理器本身。
 */
@Tag("contract")
class RealtimeAccessTokenHandshakeHandlerTests {

    /**
     * 验证 WS 路径上的 HTTP 请求会补齐 request/trace/route 上下文并继续传递给后续升级处理器。
     */
    @Test
    @DisplayName("channel read ws path stores request context and forwards request")
    void channelRead_wsPath_storesRequestContextAndForwardsRequest() {
        EmbeddedChannel channel = new EmbeddedChannel(new RealtimeAccessTokenHandshakeHandler("/api/ws"));
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/ws");

        channel.writeInbound(request);

        assertNotNull(channel.attr(RealtimeChannelSession.REQUEST_ID_KEY).get());
        assertNotNull(channel.attr(RealtimeChannelSession.TRACE_ID_KEY).get());
        assertEquals("/api/ws", channel.attr(RealtimeChannelSession.ROUTE_KEY).get());
        assertSame(request, channel.readInbound());
    }
}

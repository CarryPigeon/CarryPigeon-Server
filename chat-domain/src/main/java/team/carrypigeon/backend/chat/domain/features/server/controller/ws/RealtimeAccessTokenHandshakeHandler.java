package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.UUID;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.controller.support.HttpRequestMdcFilter;

/**
 * WebSocket 握手上下文准备处理器。
 * 职责：在 WebSocket 升级前准备请求链路上下文，认证改由首帧 `auth` 承接。
 * 边界：这里只处理升级前路由与日志上下文，不再做握手期 Bearer 鉴权。
 */
public class RealtimeAccessTokenHandshakeHandler extends ChannelInboundHandlerAdapter {

    private final String path;
    @SuppressWarnings("unused")
    private final AuthTokenService authTokenService;

    public RealtimeAccessTokenHandshakeHandler(String path, AuthTokenService authTokenService) {
        this.path = path;
        this.authTokenService = authTokenService;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) {
        if (!(message instanceof FullHttpRequest request)) {
            context.fireChannelRead(message);
            return;
        }
        if (!matchesPath(request.uri())) {
            context.fireChannelRead(message);
            return;
        }

        String requestId = resolveRequestId(request);
        String traceId = resolveTraceId(request, requestId);
        context.channel().attr(RealtimeChannelSession.REQUEST_ID_KEY).set(requestId);
        context.channel().attr(RealtimeChannelSession.TRACE_ID_KEY).set(traceId);
        context.channel().attr(RealtimeChannelSession.ROUTE_KEY).set(path);
        context.fireChannelRead(message);
    }

    private boolean matchesPath(String uri) {
        int queryIndex = uri.indexOf('?');
        String requestPath = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
        return path.equals(requestPath);
    }

    private String resolveRequestId(FullHttpRequest request) {
        String requestId = request.headers().get(HttpRequestMdcFilter.REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private String resolveTraceId(FullHttpRequest request, String requestId) {
        String traceId = request.headers().get(HttpRequestMdcFilter.TRACE_ID_HEADER);
        return traceId == null || traceId.isBlank() ? requestId : traceId;
    }
}

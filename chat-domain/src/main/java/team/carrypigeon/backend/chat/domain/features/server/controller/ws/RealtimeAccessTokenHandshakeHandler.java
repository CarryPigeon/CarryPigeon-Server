package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * WebSocket 握手 access token 校验处理器。
 * 职责：在 WebSocket 升级前校验 Bearer access token，并绑定当前通道的认证主体。
 * 边界：只处理握手期认证，不承载会话业务逻辑。
 */
public class RealtimeAccessTokenHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String path;
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

        String authorization = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorizedResponse(context, request, "authentication is required");
            return;
        }

        try {
            AuthTokenClaims claims = authTokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
            context.channel().attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY)
                    .set(new AuthenticatedPrincipal(Long.parseLong(claims.subject()), claims.username()));
            context.fireChannelRead(message);
        } catch (ProblemException exception) {
            writeUnauthorizedResponse(context, request, exception.getMessage());
        }
    }

    private boolean matchesPath(String uri) {
        int queryIndex = uri.indexOf('?');
        String requestPath = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
        return path.equals(requestPath);
    }

    private void writeUnauthorizedResponse(ChannelHandlerContext context, FullHttpRequest request, String message) {
        request.release();
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED,
                Unpooled.wrappedBuffer(payload)
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, payload.length);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}

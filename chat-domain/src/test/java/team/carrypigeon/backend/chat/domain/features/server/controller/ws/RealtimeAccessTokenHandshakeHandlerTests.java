package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * RealtimeAccessTokenHandshakeHandler 契约测试。
 * 职责：验证 WebSocket 握手阶段的 Bearer access token 校验与通道主体绑定行为。
 * 边界：不验证 Netty 协议升级细节，只验证握手鉴权处理器本身。
 */
@Tag("contract")
class RealtimeAccessTokenHandshakeHandlerTests {

    /**
     * 验证合法 Bearer access token 会在通道上绑定认证主体并放行请求。
     */
    @Test
    @DisplayName("channel read valid bearer token binds principal and forwards request")
    void channelRead_validBearerToken_bindsPrincipalAndForwardsRequest() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new RealtimeAccessTokenHandshakeHandler("/ws", new FakeAuthTokenService())
        );
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws");
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer access-token");

        channel.writeInbound(request);

        AuthenticatedPrincipal principal = channel.attr(RealtimeChannelSession.AUTHENTICATED_PRINCIPAL_KEY).get();
        assertNotNull(principal);
        assertEquals(1001L, principal.accountId());
        assertEquals("carry-user", principal.username());
        assertSame(request, channel.readInbound());
    }

    /**
     * 验证缺少 Bearer access token 会返回 401 并关闭通道。
     */
    @Test
    @DisplayName("channel read missing bearer token returns unauthorized response")
    void channelRead_missingBearerToken_returnsUnauthorizedResponse() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new RealtimeAccessTokenHandshakeHandler("/ws", new FakeAuthTokenService())
        );
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws");

        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
    }

    /**
     * 验证非法 access token 会返回 401 并拒绝握手。
     */
    @Test
    @DisplayName("channel read invalid bearer token returns unauthorized response")
    void channelRead_invalidBearerToken_returnsUnauthorizedResponse() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new RealtimeAccessTokenHandshakeHandler("/ws", new InvalidAccessTokenService())
        );
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws");
        request.headers().set(HttpHeaderNames.AUTHORIZATION, "Bearer invalid-access-token");

        channel.writeInbound(request);

        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
    }

    private static class FakeAuthTokenService implements AuthTokenService {

        @Override
        public String issueAccessToken(AuthAccount account, Instant expiresAt) {
            return "access-token";
        }

        @Override
        public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
            return "refresh-token";
        }

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            return new AuthTokenClaims("1001", "carry-user", "access", 0L, Instant.parse("2026-04-20T12:30:00Z"));
        }

        @Override
        public AuthTokenClaims parseRefreshToken(String refreshToken) {
            return new AuthTokenClaims("1001", "carry-user", "refresh", 2001L, Instant.parse("2026-05-04T12:00:00Z"));
        }
    }

    private static class InvalidAccessTokenService extends FakeAuthTokenService {

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
    }
}

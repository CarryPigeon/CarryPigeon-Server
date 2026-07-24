package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AccessTokenAuthenticationDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * RealtimeChannelHandler 测试支持。
 * 职责：为认证、心跳和事件回放测试提供不含聊天写入命令的嵌入式通道。
 */
final class RealtimeChannelHandlerTestSupport {

    private RealtimeChannelHandlerTestSupport() {
    }

    static EmbeddedChannel channel(RealtimeSessionRegistry registry) {
        JsonProvider jsonProvider = jsonProvider();
        return new EmbeddedChannel(new RealtimeChannelHandler(
                jsonProvider,
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new AccessTokenAuthenticationDomainApi(authTokenCodec()),
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                registry
        ));
    }

    static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }

    private static AuthTokenCodec authTokenCodec() {
        return new AuthTokenCodec() {
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
                if ("access-token-2".equals(accessToken)) {
                    return new AuthTokenClaims(
                            "1002", "carry-ops@example.com", "access", 0L,
                            Instant.parse("2026-04-22T00:30:00Z")
                    );
                }
                return new AuthTokenClaims(
                        "1001", "carry-user@example.com", "access", 0L,
                        Instant.parse("2026-04-22T00:30:00Z")
                );
            }

            @Override
            public AuthTokenClaims parseRefreshToken(String refreshToken) {
                return new AuthTokenClaims(
                        "1001", "carry-user@example.com", "refresh", 2001L,
                        Instant.parse("2026-05-04T12:00:00Z")
                );
            }
        };
    }
}

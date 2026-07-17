package team.carrypigeon.backend.chat.domain.features.auth.config;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.port.AuthTokenService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 鉴权 HTTP 拦截配置测试。
 * 职责：验证 `/api/**` 鉴权拦截的公开放行边界符合当前协议语义。
 * 边界：只验证路径匹配配置，不验证 JWT 解析细节。
 */
@Tag("contract")
class AuthWebMvcConfigurationTests {

    /**
     * 验证当前仅放行公开发现、gate 与公开鉴权接口，presence 等其它 `/api/**` 仍受保护。
     * 输入：最小鉴权拦截配置。
     * 输出：排除路径不再包含通配的 `/api/server/**`。
     */
    @Test
    @DisplayName("configuration excludes only public discovery catalog gate and auth routes")
    @SuppressWarnings("unchecked")
    void configuration_excludesOnlyPublicAuthRoutesAndServerEcho() throws Exception {
        AuthWebMvcConfiguration configuration = new AuthWebMvcConfiguration(new FakeAuthTokenService(), new RequestAuthenticationContext());
        InterceptorRegistry registry = new InterceptorRegistry();

        configuration.addInterceptors(registry);

        Method getInterceptorsMethod = InterceptorRegistry.class.getDeclaredMethod("getInterceptors");
        getInterceptorsMethod.setAccessible(true);
        List<Object> registrations = (List<Object>) getInterceptorsMethod.invoke(registry);
        assertThat(registrations).hasSize(1);

        MappedInterceptor interceptor = (MappedInterceptor) registrations.getFirst();
        List<String> includePatterns = List.of(interceptor.getIncludePathPatterns());
        List<String> excludePatterns = List.of(interceptor.getExcludePathPatterns());

        assertThat(includePatterns).containsExactly("/api/**");
        assertThat(excludePatterns).containsExactly(
                "/api/server",
                "/api/gates/required/check",
                "/api/plugins/catalog",
                "/api/domains/catalog",
                "/api/files/download/server_avatar",
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/email_codes",
                "/api/auth/tokens",
                "/api/auth/refresh",
                "/api/auth/revoke"
        );
        assertThat(excludePatterns).doesNotContain("/api/server/**");
    }

    /**
     * `FakeAuthTokenService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class FakeAuthTokenService implements AuthTokenService {

        @Override
        public String issueAccessToken(AuthAccount account, java.time.Instant expiresAt) {
            return "access-token";
        }

        @Override
        public String issueRefreshToken(AuthAccount account, long refreshSessionId, java.time.Instant expiresAt) {
            return "refresh-token";
        }

        @Override
        public AuthTokenClaims parseAccessToken(String accessToken) {
            return new AuthTokenClaims("1001", "carry-user", "access", 0L, java.time.Instant.parse("2026-04-20T12:30:00Z"));
        }

        @Override
        public AuthTokenClaims parseRefreshToken(String refreshToken) {
            return new AuthTokenClaims("1001", "carry-user", "refresh", 2001L, java.time.Instant.parse("2026-05-04T12:00:00Z"));
        }
    }
}

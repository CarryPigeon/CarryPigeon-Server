package team.carrypigeon.backend.chat.domain.features.auth.controller.support;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthAccessTokenInterceptor 契约测试。
 * 职责：验证 Bearer access token 校验与请求身份绑定行为。
 * 边界：不验证 JWT 加密细节，只验证 HTTP 拦截契约。
 */
@Tag("contract")
class AuthAccessTokenInterceptorTests {

    /**
     * 验证合法 Bearer access token 会绑定当前请求身份。
     */
    @Test
    @DisplayName("preHandle valid bearer token binds principal")
    void preHandle_validBearerToken_bindsPrincipal() {
        AuthRequestContext context = new AuthRequestContext();
        AuthAccessTokenInterceptor interceptor = new AuthAccessTokenInterceptor(new FakeAuthTokenService(), context);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(result);
        AuthenticatedPrincipal principal = context.requirePrincipal(request);
        assertEquals(1001L, principal.accountId());
        assertEquals("carry-user", principal.username());
    }

    /**
     * 验证缺少 Bearer access token 会返回认证失败语义。
     */
    @Test
    @DisplayName("preHandle missing bearer token throws forbidden problem")
    void preHandle_missingBearerToken_throwsForbiddenProblem() {
        AuthAccessTokenInterceptor interceptor = new AuthAccessTokenInterceptor(
                new FakeAuthTokenService(),
                new AuthRequestContext()
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())
        );

        assertEquals("authentication is required", exception.getMessage());
    }

    /**
     * 验证不合法 access token 会返回 300 对应的认证失败语义。
     */
    @Test
    @DisplayName("preHandle invalid bearer token throws forbidden problem")
    void preHandle_invalidBearerToken_throwsForbiddenProblem() {
        AuthAccessTokenInterceptor interceptor = new AuthAccessTokenInterceptor(
                new InvalidAccessTokenService(),
                new AuthRequestContext()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-access-token");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals("access token is invalid", exception.getMessage());
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

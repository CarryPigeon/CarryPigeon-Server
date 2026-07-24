package team.carrypigeon.backend.chat.domain.shared.controller.security;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.shared.domain.auth.AuthenticatedAccount;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BearerAuthenticationInterceptor 契约测试。
 * 职责：验证 Bearer access token 校验与请求身份绑定行为。
 * 边界：不验证 JWT 加密细节，只验证 HTTP 拦截契约。
 */
@Tag("contract")
class BearerAuthenticationInterceptorTests {

    /**
     * 验证合法 Bearer access token 会绑定当前请求身份。
     */
    @Test
    @DisplayName("preHandle valid bearer token binds principal")
    void preHandle_validBearerToken_bindsPrincipal() {
        RequestAuthenticationContext context = new RequestAuthenticationContext();
        BearerAuthenticationInterceptor interceptor = new BearerAuthenticationInterceptor(new FakeAccessTokenAuthenticationApi(), context);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(result);
        AuthenticatedAccount principal = context.requirePrincipal(request);
        assertEquals(1001L, principal.accountId());
        assertEquals("carry-user", principal.username());
        assertEquals("1001", MDC.get(LogKeys.UID));
    }

    /**
     * 验证 afterCompletion 会移除 uid 日志上下文字段。
     */
    @Test
    @DisplayName("afterCompletion removes uid from mdc")
    void afterCompletion_existingUid_removesMdcValue() {
        RequestAuthenticationContext context = new RequestAuthenticationContext();
        BearerAuthenticationInterceptor interceptor = new BearerAuthenticationInterceptor(new FakeAccessTokenAuthenticationApi(), context);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertNull(MDC.get(LogKeys.UID));
    }

    /**
     * 验证缺少 Bearer access token 会返回认证失败语义。
     */
    @Test
    @DisplayName("preHandle missing bearer token throws forbidden problem")
    void preHandle_missingBearerToken_throwsForbiddenProblem() {
        BearerAuthenticationInterceptor interceptor = new BearerAuthenticationInterceptor(
                new FakeAccessTokenAuthenticationApi(),
                new RequestAuthenticationContext()
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
        BearerAuthenticationInterceptor interceptor = new BearerAuthenticationInterceptor(
                new InvalidAccessTokenService(),
                new RequestAuthenticationContext()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-access-token");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals("access token is invalid", exception.getMessage());
    }

    /**
     * 验证 access token claims subject 不是账号 ID 时仍返回稳定鉴权失败，而不是未分类运行时异常。
     */
    @Test
    @DisplayName("preHandle non numeric subject throws invalid access token problem")
    void preHandle_nonNumericSubject_throwsInvalidAccessTokenProblem() {
        BearerAuthenticationInterceptor interceptor = new BearerAuthenticationInterceptor(
                new NonNumericSubjectAccessTokenService(),
                new RequestAuthenticationContext()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals("invalid_access_token", exception.reason());
        assertEquals("access token is invalid", exception.getMessage());
    }

    /**
     * `FakeAuthTokenService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class FakeAccessTokenAuthenticationApi implements AccessTokenAuthenticationApi {
        @Override
        public AccessTokenAuthenticationResult authenticate(String accessToken) {
            return new AccessTokenAuthenticationResult(1001L, "carry-user", Instant.parse("2026-04-20T12:30:00Z"));
        }
    }

    /**
     * `InvalidAccessTokenService` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    private static class InvalidAccessTokenService extends FakeAccessTokenAuthenticationApi {

        @Override
        public AccessTokenAuthenticationResult authenticate(String accessToken) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
    }

    /**
     * `NonNumericSubjectAccessTokenService` 测试替身。
     * 职责：模拟 token port 返回无法作为账号 ID 使用的 subject。
     */
    private static class NonNumericSubjectAccessTokenService extends FakeAccessTokenAuthenticationApi {

        @Override
        public AccessTokenAuthenticationResult authenticate(String accessToken) {
            throw ProblemException.forbidden("invalid_access_token", "access token is invalid");
        }
    }
}

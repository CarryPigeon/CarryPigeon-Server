package team.carrypigeon.backend.chat.domain.config.http.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.AuthTokenApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.token.result.AccessTokenAuthenticationResult;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * JWT 过滤器的错误映射测试。
 * 职责：验证认证失败返回 401，非预期异常返回 500，成功认证继续 filter chain。
 */
class JwtAuthenticationFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final AuthTokenApi authTokenApi = mock(AuthTokenApi.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            authTokenApi,
            new JsonProviderImpl(objectMapper)
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        LogContexts.clear();
    }

    @Test
    void mapsExpiredTokenToStandard401() throws Exception {
        when(authTokenApi.authenticate("expired"))
                .thenThrow(ProblemException.of(AuthProblem.TOKEN_EXPIRED));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken("expired"), response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertNull(chain.getRequest());

        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertEquals(401, error.path("status").asInt());
        assertEquals("token_expired", error.path("reason").asText());
        assertEquals("token is expired", error.path("message").asText());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void mapsUnexpectedFailureToStandard500() throws Exception {
        when(authTokenApi.authenticate("boom"))
                .thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken("boom"), response, chain);

        assertEquals(500, response.getStatus());
        assertNull(chain.getRequest());

        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertEquals(500, error.path("status").asInt());
        assertEquals("internal_error", error.path("reason").asText());
        assertEquals("internal server error", error.path("message").asText());
    }

    @Test
    void continuesFilterChainForValidToken() throws Exception {
        when(authTokenApi.authenticate("ok"))
                .thenReturn(new AccessTokenAuthenticationResult(42L, Instant.now().plusSeconds(60)));
        MockHttpServletRequest request = requestWithToken("ok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertInstanceOf(CpPrincipal.class, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(42L, ((CpPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).accountId());
    }

    @Test
    void acceptsCaseInsensitiveBearerScheme() throws Exception {
        when(authTokenApi.authenticate("ok"))
                .thenReturn(new AccessTokenAuthenticationResult(42L, Instant.now().plusSeconds(60)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer ok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertEquals(42L, ((CpPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).accountId());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}

package team.carrypigeon.backend.chat.domain.config.http.security;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 过滤器的错误映射测试。
 * 职责：验证业务入口认证失败或内部异常返回数字码，成功认证继续 filter chain。
 */
class JwtAuthenticationFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final AuthTokenApi authTokenApi = mock(AuthTokenApi.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            authTokenApi,
            SecurityErrorResponsesTests.mvcErrors()
    );

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        LogContexts.clear();
    }

    @Test
    void mapsExpiredTokenToBusinessCode() throws Exception {
        when(authTokenApi.authenticate("expired"))
                .thenThrow(ProblemException.of(AuthProblem.TOKEN_EXPIRED));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken("expired"), response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertNull(chain.getRequest());

        JsonNode error = objectMapper.readTree(response.getContentAsString());
        assertEquals(2, error.size());
        assertEquals(11002, error.get("code").intValue());
        assertEquals("token is expired", error.path("message").asText());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void mapsUnexpectedFailureToInternalError() throws Exception {
        when(authTokenApi.authenticate("boom"))
                .thenThrow(new IllegalStateException("database unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithToken("boom"), response, chain);

        assertEquals(200, response.getStatus());
        assertNull(chain.getRequest());

        JsonNode error = objectMapper.readTree(response.getContentAsString());
        assertEquals(2, error.size());
        assertEquals(10012, error.get("code").intValue());
        assertEquals("Internal server error.", error.path("message").asText());
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer ok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertEquals(42L, ((CpPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).accountId());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}

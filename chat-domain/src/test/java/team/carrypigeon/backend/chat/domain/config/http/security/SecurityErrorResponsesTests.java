package team.carrypigeon.backend.chat.domain.config.http.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import team.carrypigeon.backend.chat.domain.http.advice.GlobalExceptionHandler;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.server.controller.http.ServerController;
import team.carrypigeon.backend.chat.domain.http.security.SecurityErrorResponses;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 认证失败须按真实 MVC 路由区分业务接口、下载和协议错误。 */
class SecurityErrorResponsesTests {

    static SecurityErrorResponses mvcErrors() {
        var mvc = MockMvcBuilders.standaloneSetup(new ServerController(null), new FileController(null, null))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        var context = mvc.getDispatcherServlet().getWebApplicationContext();
        return new SecurityErrorResponses(
            context.getBean(RequestMappingHandlerMapping.class),
            context.getBean(HandlerExceptionResolver.class)
        );
    }

    @Test
    void downloadRequiresBearerChallenge() throws Exception {
        var response = reject("GET", "/api/files/download/share", new InsufficientAuthenticationException("missing"));
        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertError(response, 11001);
    }

    @Test
    void businessDenialUsesNumericCode() throws Exception {
        var response = reject("GET", "/api/server", new AccessDeniedException("denied"));
        assertEquals(200, response.getStatus());
        assertError(response, 10009);
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        var response = reject("GET", "/api/server", new IllegalStateException("private connection details"));
        assertEquals(200, response.getStatus());
        assertEquals("Internal server error.", assertError(response, 10012).get("message").textValue());
    }

    @Test
    void routeAndMethodErrorsTakePrecedenceOverAuthentication() throws Exception {
        var denied = new InsufficientAuthenticationException("missing");
        var unknown = reject("GET", "/api/unknown", denied);
        assertEquals(404, unknown.getStatus());
        assertError(unknown, 10003);
        var wrongMethod = reject("POST", "/api/server", denied);
        assertEquals(405, wrongMethod.getStatus());
        assertError(wrongMethod, 10004);
        org.junit.jupiter.api.Assertions.assertTrue(wrongMethod.getHeader("Allow").contains("GET"));
    }

    private MockHttpServletResponse reject(String method, String path, Exception exception) {
        var response = new MockHttpServletResponse();
        mvcErrors().write(new MockHttpServletRequest(method, path), response, exception);
        return response;
    }

    private JsonNode assertError(MockHttpServletResponse response, int code) throws Exception {
        JsonNode body = new ObjectMapper().readTree(response.getContentAsByteArray());
        assertEquals(2, body.size());
        assertEquals(code, body.get("code").intValue());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        return body;
    }
}

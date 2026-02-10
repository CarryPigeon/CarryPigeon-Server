package team.carrypigeon.backend.chat.domain.controller.web.api.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiVersionFilterTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doFilter_unsupportedVersion_expectedApiVersionUnsupported() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json; version=2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(CPProblemReason.API_VERSION_UNSUPPORTED.status(), response.getStatus());

        JsonNode root = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(CPProblemReason.API_VERSION_UNSUPPORTED.code(), root.path("error").path("reason").asText());
    }

    @Test
    void doFilter_supportedVersionAndJsonResponse_expectedVersionedContentType() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json; version=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            chainCalled.set(true);
            MockHttpServletResponse r = (MockHttpServletResponse) res;
            r.setStatus(200);
            r.setContentType(MediaType.APPLICATION_JSON_VALUE);
        };

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.carrypigeon+json; version=1", response.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void doFilter_acceptWithoutVersion_expectedPassThrough() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            chainCalled.set(true);
            ((MockHttpServletResponse) res).setStatus(204);
        };

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(204, response.getStatus());
    }

    @Test
    void doFilter_nonTargetMediaExpectedIgnoreVersion() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/json; version=999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            chainCalled.set(true);
            ((MockHttpServletResponse) res).setStatus(204);
        };

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(204, response.getStatus());
    }

    @Test
    void doFilter_withoutAcceptHeaderAndJsonResponse_expectedVersionedContentType() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            MockHttpServletResponse r = (MockHttpServletResponse) res;
            r.setStatus(200);
            r.setContentType(MediaType.APPLICATION_JSON_VALUE);
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.carrypigeon+json; version=1", response.getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void doFilter_nonJsonResponse_expectedKeepOriginalContentType() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json; version=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            MockHttpServletResponse r = (MockHttpServletResponse) res;
            r.setStatus(200);
            r.setContentType(MediaType.TEXT_PLAIN_VALUE);
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
    }

    @Test
    void doFilter_committedJsonResponse_expectedNoRewrite() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/server");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json; version=1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            MockHttpServletResponse r = (MockHttpServletResponse) res;
            r.setStatus(200);
            r.setContentType(MediaType.APPLICATION_JSON_VALUE);
            r.flushBuffer();
        };

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
    }

    @Test
    void doFilter_wsPath_expectedSkipVersionFilter() throws Exception {
        ApiVersionFilter filter = new ApiVersionFilter(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ws");
        request.addHeader(HttpHeaders.ACCEPT, "application/vnd.carrypigeon+json; version=999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            chainCalled.set(true);
            ((MockHttpServletResponse) res).setStatus(204);
        };

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(204, response.getStatus());
    }
}

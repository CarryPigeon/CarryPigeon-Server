package team.carrypigeon.backend.chat.domain.config.http.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.logging.LogContexts;

/**
 * 安全层错误响应测试。
 * 职责：验证 401/403/500 都使用统一的 ApiErrorResponse 结构。
 */
class SecurityErrorResponsesTests {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final JsonProviderImpl jsonProvider = new JsonProviderImpl(objectMapper);

    @AfterEach
    void clearContext() {
        LogContexts.clear();
    }

    @Test
    void writesUnauthorizedWithStandardEnvelope() throws Exception {
        LogContexts.sessionId("req-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityErrorResponses.write(
                jsonProvider, response, 401, "unauthorized", "authentication is required", null);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));

        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertEquals(401, error.path("status").asInt());
        assertEquals("unauthorized", error.path("reason").asText());
        assertEquals("authentication is required", error.path("message").asText());
        assertEquals("req-1", error.path("request_id").asText());
    }

    @Test
    void writesForbiddenWithStandardEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityErrorResponses.write(
                jsonProvider, response, 403, "forbidden", "access denied", null);

        assertEquals(403, response.getStatus());

        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertEquals(403, error.path("status").asInt());
        assertEquals("forbidden", error.path("reason").asText());
        assertEquals("access denied", error.path("message").asText());
    }

    @Test
    void writesInternalErrorWithStandardEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityErrorResponses.write(
                jsonProvider, response, 500, "internal_error", "internal server error", null);

        assertEquals(500, response.getStatus());

        JsonNode error = objectMapper.readTree(response.getContentAsString()).path("error");
        assertEquals(500, error.path("status").asInt());
        assertEquals("internal_error", error.path("reason").asText());
        assertEquals("internal server error", error.path("message").asText());
    }
}

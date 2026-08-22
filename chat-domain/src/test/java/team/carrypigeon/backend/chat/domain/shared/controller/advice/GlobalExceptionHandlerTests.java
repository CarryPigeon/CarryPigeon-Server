package team.carrypigeon.backend.chat.domain.shared.controller.advice;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 契约测试。
 * 职责：验证业务问题异常到标准 HTTP 错误响应的稳定映射。
 * 边界：不通过生产 HTTP demo 端点构造错误，只验证统一异常处理契约。
 */
@Tag("contract")
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证认证问题会映射到 401 响应码。
     * 输入：forbidden 类型业务问题异常。
     * 输出：HTTP 401，reason 为 unauthorized。
     */
    @Test
    @DisplayName("handle authentication forbidden returns status 401")
    void handleProblemException_authenticationForbidden_returnsStatus401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.forbidden("authentication_required", "authentication is required"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("unauthorized", response.getBody().error().reason());
        assertEquals("Bearer", response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE));
    }

    /**
     * 验证用户名或密码错误属于未认证问题，并隐藏具体凭据失败原因。
     */
    @Test
    @DisplayName("handle invalid credentials returns status 401")
    void handleProblemException_invalidCredentials_returnsStatus401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.forbidden("invalid_credentials", "username or password is invalid"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("unauthorized", response.getBody().error().reason());
        assertEquals("Bearer", response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE));
    }

    /**
     * 验证资源不存在问题会映射到 404 响应码。
     * 输入：not found 类型业务问题异常。
     * 输出：HTTP 状态码为 404。
     */
    @Test
    @DisplayName("handle problem not found returns status 404")
    void handleProblemException_notFound_returnsStatus404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.notFound("resource not found")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not_found", response.getBody().error().reason());
    }

    /**
     * 验证内部业务问题会映射到 500 响应码。
     * 输入：internal 类型业务问题异常。
     * 输出：HTTP 500，且不暴露内部 reason。
     */
    @Test
    @DisplayName("handle problem internal returns status 500")
    void handleProblemException_internal_returnsStatus500() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.fail("test_failure", "internal failure")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("internal_error", response.getBody().error().reason());
        assertEquals("internal server error", response.getBody().error().message());
    }

    /**
     * 验证邮件服务未就绪会映射为 503，并保留稳定 reason。
     */
    @Test
    @DisplayName("handle mail service unavailable returns status 503")
    void handleProblemException_mailServiceUnavailable_returnsStatus503() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.fail("mail_service_unavailable", "mail service is unavailable")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("mail_service_unavailable", response.getBody().error().reason());
        assertEquals("mail service is unavailable", response.getBody().error().message());
    }

    /**
     * 验证邮件投递失败会映射为 503，并保留稳定 reason。
     */
    @Test
    @DisplayName("handle email delivery failed returns status 503")
    void handleProblemException_emailDeliveryFailed_returnsStatus503() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.fail("email_delivery_failed", "failed to deliver verification email")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("email_delivery_failed", response.getBody().error().reason());
        assertEquals("failed to deliver verification email", response.getBody().error().message());
    }

    /**
     * 验证对象存储未就绪会映射为 503，并保留客户端可识别的稳定 reason。
     */
    @Test
    @DisplayName("handle storage service unavailable returns status 503")
    void handleProblemException_storageServiceUnavailable_returnsStatus503() {
        ResponseEntity<ApiErrorResponse> response = handler.handleProblemException(
                ProblemException.fail("storage_service_unavailable", "storage service is unavailable")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("storage_service_unavailable", response.getBody().error().reason());
        assertEquals("storage service is unavailable", response.getBody().error().message());
    }

    /**
     * 验证请求绑定失败会映射到 422 响应码。
     * 输入：带字段错误的 BindException。
     * 输出：HTTP 422，且消息与 field_errors 保持可读。
     */
    @Test
    @DisplayName("handle validation bind exception returns status 422")
    void handleValidationException_bindException_returnsStatus422() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "username", "username is invalid"));

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("username is invalid", response.getBody().error().message());
        assertNotNull(response.getBody().error().details());
    }

    /**
     * 验证 Java camelCase 字段在错误明细中转换为对外 snake_case。
     */
    @Test
    @DisplayName("handle validation field error uses snake case")
    void handleValidationException_camelCaseField_usesSnakeCase() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "grantType", "grant_type must not be blank"));

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(exception);

        assertEquals("grant_type", ((List<?>) response.getBody().error().details().get("field_errors"))
                .stream()
                .map(item -> ((Map<?, ?>) item).get("field"))
                .findFirst()
                .orElseThrow());
    }

    /**
     * 验证错误响应会回填请求链路 ID。
     */
    @Test
    @DisplayName("handle validation exception includes request id")
    void handleValidationException_includesRequestId() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "username", "username is invalid"));
        MDC.put(LogKeys.REQUEST_ID, "req-123");
        try {
            ResponseEntity<ApiErrorResponse> response = handler.handleValidationException(exception);
            assertEquals("req-123", response.getBody().error().requestId());
        } finally {
            MDC.clear();
        }
    }

    /**
     * 验证路径变量类型错误属于客户端校验失败，而不是服务端内部错误。
     */
    @Test
    @DisplayName("handle path variable type mismatch returns status 422")
    void handleValidationException_pathVariableTypeMismatch_returnsStatus422() throws Exception {
        protocolMockMvc().perform(get("/contract/ids/not-a-number"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"))
                .andExpect(jsonPath("$.error.message").value("id is invalid"));
    }

    /**
     * 验证缺少必需 multipart part 时返回统一 422 错误对象。
     */
    @Test
    @DisplayName("handle missing multipart part returns status 422")
    void handleValidationException_missingMultipartPart_returnsStatus422() throws Exception {
        protocolMockMvc().perform(multipart("/contract/uploads")
                        .file(new MockMultipartFile("other", "test.txt", MediaType.TEXT_PLAIN_VALUE, new byte[]{1})))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.reason").value("validation_failed"));
    }

    /**
     * 验证不受支持的请求媒体类型返回 415，而不是通用 500。
     */
    @Test
    @DisplayName("handle unsupported media type returns status 415")
    void handleUnsupportedMediaType_textPlainRequest_returnsStatus415() throws Exception {
        protocolMockMvc().perform(post("/contract/json")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.reason").value("unsupported_media_type"));
    }

    /**
     * 验证路由存在但方法不受支持时返回 405 和 Allow 响应头。
     */
    @Test
    @DisplayName("handle unsupported method returns status 405 and allow header")
    void handleMethodNotAllowed_postToGetRoute_returnsStatus405AndAllowHeader() throws Exception {
        protocolMockMvc().perform(post("/contract/ids/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"))
                .andExpect(jsonPath("$.error.reason").value("method_not_allowed"));
    }

    /**
     * 验证未知 API 路由返回统一 404 错误对象，而不是落入 500。
     */
    @Test
    @DisplayName("handle missing route returns status 404")
    void handleRouteNotFound_missingResource_returnsStatus404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRouteNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/missing")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not_found", response.getBody().error().reason());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    private MockMvc protocolMockMvc() {
        return MockMvcBuilders.standaloneSetup(new ProtocolExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 仅用于触发 Spring MVC 协议异常的最小测试控制器。
     */
    @RestController
    private static class ProtocolExceptionController {

        @GetMapping("/contract/ids/{id}")
        long getById(@PathVariable long id) {
            return id;
        }

        @PostMapping(path = "/contract/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        long upload(@RequestPart("file") MultipartFile file) {
            return file.getSize();
        }

        @PostMapping(path = "/contract/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        void json() {
        }
    }
}

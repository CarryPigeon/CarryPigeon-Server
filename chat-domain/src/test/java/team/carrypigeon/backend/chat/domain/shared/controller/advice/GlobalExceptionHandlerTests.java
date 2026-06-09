package team.carrypigeon.backend.chat.domain.shared.controller.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * GlobalExceptionHandler 契约测试。
 * 职责：验证业务问题异常到标准 HTTP 错误响应的稳定映射。
 * 边界：不通过生产 HTTP demo 端点构造错误，只验证统一异常处理契约。
 */
@Tag("contract")
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证权限问题会映射到 300 响应码。
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
}

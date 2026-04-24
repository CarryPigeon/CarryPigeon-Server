package team.carrypigeon.backend.chat.domain.shared.controller.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GlobalExceptionHandler 契约测试。
 * 职责：验证业务问题异常到 CPResponse 响应码的稳定映射。
 * 边界：不通过生产 HTTP demo 端点构造错误，只验证统一异常处理契约。
 */
@Tag("contract")
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证权限问题会映射到 300 响应码。
     * 输入：forbidden 类型业务问题异常。
     * 输出：统一响应码为 300。
     */
    @Test
    @DisplayName("handle problem forbidden returns code 300")
    void handleProblemException_forbidden_returnsCode300() {
        CPResponse<Void> response = handler.handleProblemException(
                ProblemException.forbidden("test_forbidden", "forbidden")
        );

        assertEquals(300, response.code());
    }

    /**
     * 验证资源不存在问题会映射到 404 响应码。
     * 输入：not found 类型业务问题异常。
     * 输出：统一响应码为 404。
     */
    @Test
    @DisplayName("handle problem not found returns code 404")
    void handleProblemException_notFound_returnsCode404() {
        CPResponse<Void> response = handler.handleProblemException(
                ProblemException.notFound("resource not found")
        );

        assertEquals(404, response.code());
    }

    /**
     * 验证内部业务问题会映射到 500 响应码。
     * 输入：internal 类型业务问题异常。
     * 输出：统一响应码为 500。
     */
    @Test
    @DisplayName("handle problem internal returns code 500")
    void handleProblemException_internal_returnsCode500() {
        CPResponse<Void> response = handler.handleProblemException(
                ProblemException.fail("test_failure", "internal failure")
        );

        assertEquals(500, response.code());
    }

    /**
     * 验证请求绑定失败会映射到 200 响应码。
     * 输入：带字段错误的 BindException。
     * 输出：统一响应码为 200，且消息保持可读。
     */
    @Test
    @DisplayName("handle validation bind exception returns code 200")
    void handleValidationException_bindException_returnsCode200() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "username", "username is invalid"));

        CPResponse<Void> response = handler.handleValidationException(exception);

        assertEquals(200, response.code());
        assertEquals("username is invalid", response.message());
    }
}

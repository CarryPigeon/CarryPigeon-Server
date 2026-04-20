package team.carrypigeon.backend.chat.domain.shared.controller.advice;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 全局异常映射入口。
 * 职责：把业务问题异常和常见请求异常收敛为稳定的 CPResponse。
 * 边界：这里只负责协议层映射，不承载业务决策逻辑。
 */
@Slf4j
@RestControllerAdvice(basePackages = "team.carrypigeon.backend.chat.domain")
public class GlobalExceptionHandler {

    /**
     * 处理业务问题异常。
     *
     * @param exception 业务问题异常
     * @return 稳定响应对象
     */
    @ExceptionHandler(ProblemException.class)
    public CPResponse<Void> handleProblemException(ProblemException exception) {
        return switch (exception.type()) {
            case VALIDATION -> CPResponse.validationFailed(exception.getMessage());
            case FORBIDDEN -> CPResponse.forbidden(exception.getMessage());
            case NOT_FOUND -> CPResponse.notFound(exception.getMessage());
            case INTERNAL -> CPResponse.fail(exception.getMessage());
        };
    }

    /**
     * 处理请求绑定与校验异常。
     *
     * @param exception 请求校验异常
     * @return 参数错误响应
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public CPResponse<Void> handleValidationException(Exception exception) {
        return CPResponse.validationFailed(resolveValidationMessage(exception));
    }

    /**
     * 处理未显式分类的异常。
     *
     * @param exception 未捕获异常
     * @return 系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public CPResponse<Void> handleUnexpectedException(Exception exception) {
        log.error("Unexpected exception captured by global handler", exception);
        return CPResponse.fail("internal server error");
    }

    private String resolveValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().getFieldError() != null) {
            return methodArgumentNotValidException.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (exception instanceof BindException bindException && bindException.getFieldError() != null) {
            return bindException.getFieldError().getDefaultMessage();
        }
        if (exception instanceof ConstraintViolationException constraintViolationException
                && !constraintViolationException.getConstraintViolations().isEmpty()) {
            return constraintViolationException.getConstraintViolations().iterator().next().getMessage();
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "request body is invalid";
        }
        return "validation failed";
    }
}

package team.carrypigeon.backend.chat.domain.shared.controller.advice;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiError;
import team.carrypigeon.backend.chat.domain.shared.controller.error.ApiErrorResponse;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.logging.LogKeys;

/**
 * 全局异常映射入口。
 * 职责：把业务问题异常和常见请求异常收敛为稳定的 HTTP 错误响应。
 * 边界：这里只负责协议层映射，不承载业务决策逻辑。
 */
@RestControllerAdvice(basePackages = "team.carrypigeon.backend.chat.domain")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务问题异常。
     *
     * @param exception 业务问题异常
     * @return 稳定响应对象
     */
    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ApiErrorResponse> handleProblemException(ProblemException exception) {
        ErrorDescriptor descriptor = switch (exception.type()) {
            case VALIDATION -> validationDescriptor(exception);
            case CONFLICT -> new ErrorDescriptor(HttpStatus.CONFLICT, exception.reason(), exception.getMessage(), exception.details());
            case FORBIDDEN -> forbiddenDescriptor(exception);
            case NOT_FOUND -> new ErrorDescriptor(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), null);
            case INTERNAL -> internalDescriptor(exception);
        };
        if (exception.type() == team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemType.INTERNAL) {
            log.error("Problem exception mapped to internal error, reason={}", exception.reason(), exception);
        }
        return buildErrorResponse(descriptor);
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
    public ResponseEntity<ApiErrorResponse> handleValidationException(Exception exception) {
        return buildErrorResponse(new ErrorDescriptor(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "validation_failed",
                resolveValidationMessage(exception),
                resolveValidationDetails(exception)
        ));
    }

    /**
     * 处理未显式分类的异常。
     *
     * @param exception 未捕获异常
     * @return 系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unexpected exception captured by global handler", exception);
        return buildErrorResponse(new ErrorDescriptor(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "internal server error",
                null
        ));
    }

    /**
     * 从常见请求校验异常中提取客户端可读消息。
     * 语义：优先返回字段级校验消息，无法定位字段时使用稳定兜底消息。
     *
     * @param exception 请求绑定或校验异常
     * @return 客户端可读校验失败消息
     */
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

    /**
     * 从请求校验异常中提取字段错误明细。
     * 输出：仅字段级异常返回 `field_errors`，无法稳定表达字段时返回 null。
     *
     * @param exception 请求绑定或校验异常
     * @return 错误明细，缺失时为 null
     */
    private Map<String, Object> resolveValidationDetails(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return Map.of("field_errors", methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(this::toFieldError)
                    .toList());
        }
        if (exception instanceof BindException bindException) {
            return Map.of("field_errors", bindException.getFieldErrors().stream()
                    .map(this::toFieldError)
                    .toList());
        }
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            List<Map<String, String>> fieldErrors = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> Map.of(
                            "field", violation.getPropertyPath().toString(),
                            "reason", "invalid",
                            "message", violation.getMessage()
                    ))
                    .toList();
            return fieldErrors.isEmpty() ? null : Map.of("field_errors", fieldErrors);
        }
        return null;
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "reason", "invalid",
                "message", fieldError.getDefaultMessage() == null ? "validation failed" : fieldError.getDefaultMessage()
        );
    }

    /**
     * 映射鉴权与权限类领域问题到 HTTP 错误描述。
     * 约束：token 与登录态问题统一映射为 401，领域权限不足映射为 403。
     *
     * @param exception 领域问题异常
     * @return HTTP 错误描述
     */
    private ErrorDescriptor forbiddenDescriptor(ProblemException exception) {
        return switch (exception.reason()) {
            case "authentication_required", "invalid_access_token", "invalid_refresh_token", "invalid_token" ->
                    new ErrorDescriptor(HttpStatus.UNAUTHORIZED, "unauthorized", exception.getMessage(), null);
            case "token_expired" ->
                    new ErrorDescriptor(HttpStatus.UNAUTHORIZED, "token_expired", exception.getMessage(), null);
            case "invalid_credentials",
                 "private_channel_required",
                 "channel_invite_forbidden",
                 "channel_profile_forbidden",
                 "channel_role_forbidden",
                 "channel_ownership_forbidden",
                 "channel_ban_forbidden",
                 "channel_pin_forbidden",
                 "channel_message_recall_forbidden",
                 "system_channel_membership_required",
                 "system_channel_members_hidden",
                 "system_channel_required",
                 "message_not_editable",
                 "message_edit_window_expired" ->
                    new ErrorDescriptor(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), null);
            default -> new ErrorDescriptor(HttpStatus.FORBIDDEN, exception.reason(), exception.getMessage(), null);
        };
    }

    /**
     * 映射校验类领域问题到 HTTP 错误描述。
     * 约束：少量具有业务状态语义的校验问题会提升为 409 或 412，其余保持 422。
     *
     * @param exception 领域问题异常
     * @return HTTP 错误描述
     */
    private ErrorDescriptor validationDescriptor(ProblemException exception) {
        if ("required_plugin_missing".equals(exception.reason())) {
            return new ErrorDescriptor(
                    HttpStatus.PRECONDITION_FAILED,
                    exception.reason(),
                    exception.getMessage(),
                    exception.details()
            );
        }
        if ("application_already_processed".equals(exception.reason())) {
            return new ErrorDescriptor(
                    HttpStatus.CONFLICT,
                    "conflict",
                    exception.getMessage(),
                    exception.details()
            );
        }
        return new ErrorDescriptor(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.reason(),
                exception.getMessage(),
                exception.details()
        );
    }

    /**
     * 映射内部类领域问题到 HTTP 错误描述。
     * 约束：可被调用方理解的外部服务不可用返回 503，其余内部问题统一隐藏为 500。
     *
     * @param exception 领域问题异常
     * @return HTTP 错误描述
     */
    private ErrorDescriptor internalDescriptor(ProblemException exception) {
        return switch (exception.reason()) {
            case "mail_service_unavailable", "email_delivery_failed" ->
                    new ErrorDescriptor(HttpStatus.SERVICE_UNAVAILABLE, exception.reason(), exception.getMessage(), null);
            default -> new ErrorDescriptor(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "internal server error", null);
        };
    }

    /**
     * 构造统一 HTTP 错误响应。
     * 约束：响应体必须带 requestId，便于客户端错误和服务端日志关联。
     *
     * @param descriptor HTTP 错误描述
     * @return 可直接返回给客户端的响应实体
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(ErrorDescriptor descriptor) {
        ApiErrorResponse response = new ApiErrorResponse(new ApiError(
                descriptor.status().value(),
                descriptor.reason(),
                descriptor.message(),
                MDC.get(LogKeys.REQUEST_ID),
                descriptor.details()
        ));
        return ResponseEntity.status(descriptor.status()).body(response);
    }

    /**
     * HTTP 错误响应描述。
     * 职责：在异常映射阶段承载状态码、稳定原因、客户端消息和可选明细。
     *
     * @param status HTTP 状态码
     * @param reason 稳定错误原因
     * @param message 客户端可读错误消息
     * @param details 错误明细，可为空
     */
    private record ErrorDescriptor(
            HttpStatus status,
            String reason,
            String message,
            Map<String, Object> details
    ) {
    }
}

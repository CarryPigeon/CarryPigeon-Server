package team.carrypigeon.backend.chat.domain.http.exception;

import com.fasterxml.jackson.core.exc.StreamReadException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import team.carrypigeon.backend.chat.domain.http.log.HttpRequestLoggingFilter;
import team.carrypigeon.backend.chat.domain.features.verification.domain.problem.VerificationProblem;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import team.carrypigeon.backend.chat.domain.shared.result.ErrorResult;
import team.carrypigeon.backend.chat.domain.shared.result.Result;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/** MVC 与 Security 共用的 HTTP 错误映射。根据匹配到的入口选择传输状态，领域错误保留稳定数字码。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获到项目的异常代码，进行统一处理
     * */
    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ErrorResult> problem(ProblemException ex) {
        int code = ex.mapping().code();
        if (code == 10011 || code == 10012 || ex.mapping() == VerificationProblem.EMAIL_DELIVERY_FAILED) {
            var event = log.atError()
                .addKeyValue("business_code", code)
                .addKeyValue("reason", ex.mapping());
            // 邮件和依赖异常的消息可包含收件人或请求参数，只记录原始故障类型。
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
            event.addKeyValue("failure_type", cause.getClass().getName())
                .log("HTTP 业务依赖或内部操作失败");
        }
        return failure(code, ex.getMessage(), new HttpHeaders());
    }

    /**
     * 处理权限校验异常
     * */
    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResult> security(RuntimeException ex) {
        if (ex instanceof AuthenticationException) {
            return failure(AuthProblem.AUTHENTICATION_REQUIRED.code(), "Authentication is required.");
        }
        return failure(10009, "Access denied.");
    }

    /**
     * 处理请求体不可读异常
     * */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResult> unreadable(HttpMessageNotReadableException ex) {
        if (ex.contains(StreamReadException.class)) {
            return failure(10002, "Malformed JSON.");
        }
        return failure(10001, "Request body is missing or contains an invalid value.");
    }

    /**
     * 处理参数验证异常
     * */
    @ExceptionHandler({
        BindException.class, ConstraintViolationException.class, ServletRequestBindingException.class,
        MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResult> invalid() {
        return failure(10001, "Parameter validation failed.");
    }

    /**
     * 处理 HTTP 协议异常
     * */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotAcceptableException.class,
        HttpMediaTypeNotSupportedException.class, MaxUploadSizeExceededException.class,
        NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResult> protocol(Exception ex) {
        ErrorResponse error = (ErrorResponse) ex;
        int status = error.getStatusCode().value();
        return switch (status) {
            case 404 -> failure(10003, "Route not found.");
            case 405 -> failure(10004, "Method not allowed.", error.getHeaders());
            case 406 -> failure(10005, "Requested response media type is not supported.");
            case 413 -> failure(10008, "Request payload is too large.");
            case 415 -> failure(10006, "Request media type is not supported.");
            default -> throw new IllegalStateException("Unexpected protocol status " + status, ex);
        };
    }

    /**
     * 处理数据库悲观锁冲突异常
     * */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResult> lockConflict(PessimisticLockingFailureException ex) {
        return failure(10010, "Concurrent modification. Retry the operation.");
    }

    /**
     * 处理数据库不可用异常
     * */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ErrorResult> unavailable(RuntimeException ex) {
        log.atError()
            .addKeyValue("business_code", 10011)
            .addKeyValue("dependency", "database")
            .setCause(ex)
            .log("数据库不可用");
        return failure(10011, "Service temporarily unavailable.");
    }

    /**
     * 处理其他异常
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> unexpected(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        var event = log.atError()
            .setCause(ex);
        if (response.isCommitted()) {
            event.addKeyValue("stage", "response_write")
                .log("HTTP 响应已提交，处理失败");
            request.setAttribute(HttpRequestLoggingFilter.WRITE_FAILED, true);
            return null;
        }
        event.addKeyValue("business_code", 10012)
            .log("HTTP 请求处理异常");
        return failure(10012, "Internal server error.");
    }

    /**
     * 构建失败响应
     * */
    private ResponseEntity<ErrorResult> failure(int code, String message) {
        return failure(code, message, new HttpHeaders());
    }

    /**
     * 构建失败响应
     * */
    private ResponseEntity<ErrorResult> failure(int code, String message, HttpHeaders headers) {
        var response = ResponseEntity.status(200).headers(headers).cacheControl(CacheControl.noStore())
            .contentType(MediaType.APPLICATION_JSON);
        return response.body(Result.failure(code, message));
    }
}

package team.carrypigeon.backend.chat.domain.http.advice;

import com.fasterxml.jackson.core.exc.StreamReadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import team.carrypigeon.backend.chat.domain.http.ErrorResult;
import team.carrypigeon.backend.chat.domain.http.Result;
import team.carrypigeon.backend.chat.domain.features.auth.domain.problem.AuthProblem;
import team.carrypigeon.backend.chat.domain.features.file.domain.problem.FileProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/** MVC 与 Security 共用的 HTTP 错误映射。根据匹配到的入口选择传输状态，领域错误保留稳定数字码。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ErrorResult> problem(ProblemException ex, HttpServletRequest request) {
        //TODO 重写
        return failure(businessStatus(request,200), ex.mapping().code(), ex.getMessage(), new HttpHeaders());
    }

    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResult> security(RuntimeException ex, HttpServletRequest request) {
        if (ex instanceof AuthenticationException) {
            return failure(businessStatus(request, 401), AuthProblem.AUTHENTICATION_REQUIRED.code(), "Authentication is required.");
        }
        return failure(businessStatus(request, 403), 10009, "Access denied.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResult> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.contains(StreamReadException.class)) {
            return failure(400, 10002, "Malformed JSON.");
        }
        return failure(businessStatus(request, 422), 10001, "Request body is missing or contains an invalid value.");
    }

    @ExceptionHandler({
        BindException.class, ConstraintViolationException.class, ServletRequestBindingException.class,
        MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResult> invalid(Exception ex, HttpServletRequest request) {
        return failure(businessStatus(request, 422), 10001, ValidationMessages.first(ex));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotAcceptableException.class,
        HttpMediaTypeNotSupportedException.class, MaxUploadSizeExceededException.class,
        NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResult> protocol(Exception ex) {
        ErrorResponse error = (ErrorResponse) ex;
        int status = error.getStatusCode().value();
        return switch (status) {
            case 404 -> failure(404, 10003, "Route not found.");
            case 405 -> failure(405, 10004, "Method not allowed.", error.getHeaders());
            case 406 -> failure(406, 10005, "Requested response media type is not supported.");
            case 413 -> failure(413, 10008, "Request payload is too large.");
            case 415 -> failure(415, 10006, "Request media type is not supported.");
            default -> throw new IllegalStateException("Unexpected protocol status " + status, ex);
        };
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResult> lockConflict(PessimisticLockingFailureException ex, HttpServletRequest request) {
        log.info("Database lock conflict", ex);
        return failure(businessStatus(request, 409), 10010, "Concurrent modification. Retry the operation.");
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ErrorResult> unavailable(RuntimeException ex, HttpServletRequest request) {
        log.error("Database unavailable", ex);
        return failure(businessStatus(request, 503), 10011, "Service temporarily unavailable.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> unexpected(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Unexpected HTTP failure", ex);
        if (response.isCommitted()) {
            // 下载已写出字节，不能再向同一响应追加 JSON。
            return null;
        }
        return failure(businessStatus(request, 500), 10012, "Internal server error.");
    }

    private static String pattern(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
    }

    private static int businessStatus(HttpServletRequest request, int original) {
        String route = pattern(request);
        return route.startsWith("/api/") && !route.startsWith("/api/files/download/") ? 200 : original;
    }

    private ResponseEntity<ErrorResult> failure(int status, int code, String message) {
        return failure(status, code, message, new HttpHeaders());
    }

    private ResponseEntity<ErrorResult> failure(int status, int code, String message, HttpHeaders headers) {
        log.info("HTTP failure status={} code={}", status, code);
        var response = ResponseEntity.status(status).headers(headers).cacheControl(CacheControl.noStore())
            .contentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return response.body(Result.failure(code, message));
    }
}

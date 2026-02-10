package team.carrypigeon.backend.chat.domain.controller.web.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * `/api` 控制层统一异常处理器。
 * <p>
 * 负责将业务异常、参数异常和系统异常统一映射为标准错误响应结构。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理 `CPProblemException` 业务异常。
     *
     * @param ex 抛出的业务异常。
     * @param request HTTP 请求对象。
     * @return 标准错误响应。
     */
    @ExceptionHandler(CPProblemException.class)
    public ResponseEntity<ApiErrorResponse> handleProblem(CPProblemException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        CPProblem p = ex.getProblem();
        if (p == null) {
            CPProblemReason reason = CPProblemReason.INTERNAL_ERROR;
            ApiErrorBody body = new ApiErrorBody(reason.status(), reason.code(), "internal error", requestId, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(body));
        }
        ApiErrorBody body = new ApiErrorBody(p.status(), p.reason().code(), p.message(), requestId, p.details());
        return ResponseEntity.status(p.status()).body(new ApiErrorResponse(body));
    }

    /**
     * 处理 Bean Validation 参数校验异常。
     *
     * @param ex 参数校验异常。
     * @param request HTTP 请求对象。
     * @return 标准校验失败响应。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("field", err.getField());
                    m.put("reason", "invalid");
                    m.put("message", err.getDefaultMessage());
                    return m;
                })
                .toList();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field_errors", fieldErrors);
        CPProblemReason reason = CPProblemReason.VALIDATION_FAILED;
        ApiErrorBody body = new ApiErrorBody(reason.status(), reason.code(), "validation failed", requestId, details);
        return ResponseEntity.unprocessableEntity().body(new ApiErrorResponse(body));
    }

    /**
     * 处理方法参数类型不匹配异常。
     *
     * @param ex 参数类型不匹配异常。
     * @param request HTTP 请求对象。
     * @return 标准校验失败响应。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        CPProblemReason reason = CPProblemReason.VALIDATION_FAILED;
        ApiErrorBody body = new ApiErrorBody(reason.status(), reason.code(), "validation failed", requestId, null);
        return ResponseEntity.unprocessableEntity().body(new ApiErrorResponse(body));
    }

    /**
     * 处理兜底异常。
     *
     * @param ex 未分类异常。
     * @param request HTTP 请求对象。
     * @return 标准内部错误响应。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        String requestId = requestId(request);
        CPProblemReason reason = CPProblemReason.INTERNAL_ERROR;
        ApiErrorBody body = new ApiErrorBody(reason.status(), reason.code(), "internal error", requestId, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(body));
    }

    /**
     * 读取或生成请求追踪 ID。
     *
     * @param request HTTP 请求对象。
     * @return 请求追踪 ID。
     */
    private String requestId(HttpServletRequest request) {
        String existing = (String) request.getAttribute("cp_request_id");
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String generated = "req_" + UUID.randomUUID();
        request.setAttribute("cp_request_id", generated);
        return generated;
    }
}

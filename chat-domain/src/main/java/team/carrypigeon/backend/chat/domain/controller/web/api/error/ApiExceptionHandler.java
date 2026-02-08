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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception mapping for HTTP `/api` endpoints.
 * <p>
 * Goals:
 * <ul>
 *   <li>Always return unified {@link ApiErrorResponse} body for predictable client handling.</li>
 *   <li>Generate a {@code request_id} for correlation if upstream did not provide one.</li>
 *   <li>Convert Spring validation errors to {@code 422 validation_failed} with field error details.</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CPProblemException.class)
    public ResponseEntity<ApiErrorResponse> handleProblem(CPProblemException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        CPProblem p = ex.getProblem();
        if (p == null) {
            ApiErrorBody body = new ApiErrorBody(500, "internal_error", "internal error", requestId, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(body));
        }
        ApiErrorBody body = new ApiErrorBody(p.status(), p.reason(), p.message(), requestId, p.details());
        return ResponseEntity.status(p.status()).body(new ApiErrorResponse(body));
    }

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
        ApiErrorBody body = new ApiErrorBody(422, "validation_failed", "validation failed", requestId, details);
        return ResponseEntity.unprocessableEntity().body(new ApiErrorResponse(body));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requestId = requestId(request);
        ApiErrorBody body = new ApiErrorBody(422, "validation_failed", "validation failed", requestId, null);
        return ResponseEntity.unprocessableEntity().body(new ApiErrorResponse(body));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        String requestId = requestId(request);
        ApiErrorBody body = new ApiErrorBody(500, "internal_error", "internal error", requestId, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(body));
    }

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

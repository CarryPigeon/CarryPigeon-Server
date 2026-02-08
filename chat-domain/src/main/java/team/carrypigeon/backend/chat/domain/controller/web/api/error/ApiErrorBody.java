package team.carrypigeon.backend.chat.domain.controller.web.api.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified error payload for HTTP `/api` endpoints.
 * <p>
 * This object is always nested under {@code {"error": ...}} via {@link ApiErrorResponse}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorBody {
    private int status;
    private String reason;
    private String message;
    private String requestId;
    private Object details;
}

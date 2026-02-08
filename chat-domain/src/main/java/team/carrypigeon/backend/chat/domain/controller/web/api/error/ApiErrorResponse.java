package team.carrypigeon.backend.chat.domain.controller.web.api.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response wrapper for HTTP `/api` endpoints.
 * <p>
 * Contract: controller/advice always returns {@code {"error": {...}}} on failure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private ApiErrorBody error;
}

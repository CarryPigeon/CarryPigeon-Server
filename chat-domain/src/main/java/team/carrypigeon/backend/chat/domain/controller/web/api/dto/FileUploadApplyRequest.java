package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/files/uploads}.
 * <p>
 * JSON fields are snake_case (configured by Spring Jackson):
 * <ul>
 *   <li>{@code mime_type} -> {@link #mimeType()}</li>
 *   <li>{@code size_bytes} -> {@link #sizeBytes()}</li>
 * </ul>
 */
public record FileUploadApplyRequest(
        @NotBlank String filename,
        String mimeType,
        @NotNull @Min(1) Long sizeBytes,
        String sha256,
        /**
         * Optional access scope for download permission.
         * <p>
         * Allowed values: {@code OWNER|AUTH|CHANNEL|PUBLIC}.
         * Default: {@code OWNER}.
         */
        String scope,
        /**
         * Optional channel id when {@code scope="CHANNEL"}.
         */
        String scopeCid
) {
}

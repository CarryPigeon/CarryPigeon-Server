package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/email_codes}.
 * <p>
 * JSON field: {@code email}.
 */
public record SendEmailCodeRequest(@NotBlank @Email String email) {
}

package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Token exchange request body for {@code POST /api/auth/tokens}.
 * <p>
 * JSON fields use snake_case:
 * {@code grant_type}, {@code email}, {@code code}, {@code client}.
 */
public record TokenRequest(
        @NotBlank String grantType,
        @NotBlank @Email String email,
        @NotBlank String code,
        @NotNull @Valid ClientInfo client
) {
}

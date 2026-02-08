package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Refresh token request body for {@code POST /api/auth/refresh}.
 * <p>
 * JSON fields: {@code refresh_token}, {@code client}.
 */
public record RefreshRequest(@NotBlank String refreshToken, @NotNull @Valid ClientInfo client) {
}

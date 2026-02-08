package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Revoke refresh token request body for {@code POST /api/auth/revoke}.
 */
public record RevokeRequest(@NotBlank String refreshToken, @NotNull @Valid ClientInfo client) {
}

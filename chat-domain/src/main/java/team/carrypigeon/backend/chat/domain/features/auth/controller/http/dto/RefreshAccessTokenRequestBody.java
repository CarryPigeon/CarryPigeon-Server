package team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 刷新 access token 请求。
 */
public record RefreshAccessTokenRequestBody(
        @Schema(description = "refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "refresh_token must not be blank")
        String refreshToken
) {
}

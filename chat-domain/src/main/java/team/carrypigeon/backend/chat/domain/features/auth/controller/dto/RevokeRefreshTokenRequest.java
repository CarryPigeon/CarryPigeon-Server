package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 撤销 refresh token 请求。
 * 职责：承载 `POST /api/auth/revoke` 的最小输入。
 * 边界：当前仅按 refresh token 与设备标识定位会话。
 */
public record RevokeRefreshTokenRequest(
        @Schema(description = "待撤销的 refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "refresh_token must not be blank")
        String refreshToken,
        @Schema(description = "客户端上下文")
        @Valid @NotNull(message = "client must not be null")
        ClientRequest client
) {

    public record ClientRequest(
            @Schema(description = "稳定设备标识", example = "a-stable-device-id")
            @NotBlank(message = "device_id must not be blank")
            String deviceId
    ) {
    }
}

package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 刷新 access token 请求。
 * 职责：承载 `POST /api/auth/refresh` 的 v1 最小输入。
 * 边界：当前仅消费 refresh token 与设备标识，不承载其它登录态字段。
 */
public record RefreshAccessTokenRequest(
        @Schema(description = "refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "refresh_token must not be blank")
        String refreshToken,
        @Schema(description = "客户端上下文")
        @Valid @NotNull(message = "client must not be null")
        ClientRequest client
) {

    /**
     * 刷新令牌请求中的客户端上下文。
     * 职责：携带稳定设备标识，用于校验 refresh token 归属。
     */
    public record ClientRequest(
            @Schema(description = "稳定设备标识", example = "a-stable-device-id")
            @NotBlank(message = "device_id must not be blank")
            String deviceId
    ) {
    }
}

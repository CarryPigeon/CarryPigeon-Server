package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 撤销 refresh token 请求。
 * 职责：承载 `POST /api/auth/revoke` 的最小输入。
 * 边界：当前仅按 refresh token 定位会话；设备标识保留为可选客户端上下文字段。
 */
public record RevokeRefreshTokenRequest(
        @Schema(description = "待撤销的 refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "refresh_token must not be blank")
        String refreshToken,
        @Schema(description = "客户端上下文")
        @Valid @NotNull(message = "client must not be null")
        ClientRequest client
) {

    /**
     * 撤销令牌请求中的客户端上下文。
     * 职责：携带可选稳定设备标识；当前版本不参与会话定位。
     */
    public record ClientRequest(
            @Schema(description = "可选稳定设备标识；当前版本不参与 refresh session 定位", example = "a-stable-device-id")
            String deviceId
    ) {
    }
}

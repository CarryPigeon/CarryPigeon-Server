package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 注销请求。
 * 职责：承载撤销 refresh session 的最小输入。
 * 边界：当前阶段仅按 refresh token 定位会话。
 *
 * @param refreshToken 待撤销 refresh token
 */
public record LogoutRequest(
        @Schema(description = "待撤销的 refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "refresh token must not be blank")
        String refreshToken
) {
}

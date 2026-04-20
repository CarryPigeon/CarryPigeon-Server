package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 * 职责：承载 HTTP refresh 入口的最小输入。
 * 边界：不包含客户端设备或验证码字段。
 *
 * @param refreshToken refresh token
 */
public record RefreshTokenRequest(@NotBlank(message = "refresh token must not be blank") String refreshToken) {
}

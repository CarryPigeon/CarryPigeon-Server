package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 令牌刷新请求体。
 *
 * @param refreshToken 刷新令牌。
 * @param client 客户端信息。
 */
public record RefreshRequest(@NotBlank String refreshToken, @NotNull @Valid ClientInfo client) {
}

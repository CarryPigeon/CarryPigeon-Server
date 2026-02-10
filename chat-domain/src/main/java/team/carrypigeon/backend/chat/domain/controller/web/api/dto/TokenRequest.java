package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 令牌签发请求体。
 *
 * @param grantType 授权类型。
 * @param email 邮箱地址。
 * @param code 邮箱验证码。
 * @param client 客户端信息。
 */
public record TokenRequest(@NotBlank String grantType,
                           @NotBlank @Email String email,
                           @NotBlank String code,
                           @NotNull @Valid ClientInfo client) {
}

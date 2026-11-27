package team.carrypigeon.backend.chat.domain.features.auth.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户名密码登录请求。
 */
public record PWDLoginRequestBody(
        @Schema(description = "用户名", example = "carry-user")
        @NotBlank(message = "username must not be blank")
        @Size(max = 320, message = "username length must be less than or equal to 320")
        String username,
        @Schema(description = "密码", example = "password123")
        @NotBlank(message = "password must not be blank")
        String password
) {
}

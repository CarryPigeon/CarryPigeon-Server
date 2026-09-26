package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户名密码注册请求。
 * 职责：承载 `POST /api/auth/register` 的最小输入。
 * 边界：只负责协议层输入校验，不承载资料初始化或会话签发语义。
 */
public record RegisterRequest(
        @Schema(description = "待注册用户名", example = "carry-user")
        @NotBlank(message = "username must not be blank")
        @Size(max = 320, message = "username length must be less than or equal to 320")
        String username,
        @Schema(description = "待注册密码", example = "password123")
        @NotBlank(message = "password must not be blank")
        @Size(max = 320, message = "password length must be less than or equal to 320")
        String password,
        @Schema(description = "待注册邮箱", example = "user@example.com")
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        @Size(max = 320, message = "email length must be less than or equal to 320")
        String email,
        @Schema(description = "待注册验证码", example = "123456")
        @NotBlank(message = "code must not be blank")
        @Pattern(
            regexp = "[0-9]{6}",
            message = "code must be exactly 6 digits"
        )
        @Size(max = 6,min = 6, message = "code must be exactly 6 characters")
        String code
) {
}

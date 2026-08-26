package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新当前账号邮箱请求。
 * 职责：承载 `PUT /api/users/me/email` 的协议输入校验。
 */
public record UpdateCurrentAccountEmailRequest(
        @Schema(description = "新邮箱", example = "new@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        @Size(max = 320, message = "email length must be less than or equal to 320")
        String email,
        @Schema(description = "邮箱验证码", example = "123456")
        @NotBlank(message = "code must not be blank")
        String code
) {
}

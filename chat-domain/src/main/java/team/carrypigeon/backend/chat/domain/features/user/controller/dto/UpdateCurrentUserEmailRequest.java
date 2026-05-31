package team.carrypigeon.backend.chat.domain.features.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新当前用户邮箱请求。
 * 职责：承载 `PUT /api/users/me/email` 的最小输入。
 * 边界：只负责协议层输入校验，不承载验证码业务逻辑。
 */
public record UpdateCurrentUserEmailRequest(
        @Schema(description = "新邮箱", example = "new@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        String email,
        @Schema(description = "邮箱验证码", example = "123456")
        @NotBlank(message = "code must not be blank")
        String code
) {
}

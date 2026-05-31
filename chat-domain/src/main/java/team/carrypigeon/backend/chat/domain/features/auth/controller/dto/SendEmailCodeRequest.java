package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码请求。
 * 职责：承载 `POST /api/auth/email_codes` 的最小输入。
 * 边界：只包含邮箱字段，不承载客户端设备或鉴权信息。
 */
public record SendEmailCodeRequest(
        @Schema(description = "目标邮箱", example = "user@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        String email
) {
}

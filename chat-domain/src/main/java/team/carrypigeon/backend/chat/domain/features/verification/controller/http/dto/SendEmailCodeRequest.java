package team.carrypigeon.backend.chat.domain.features.verification.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.verification.EmailCodePurpose;

/**
 * 发送邮箱验证码请求。
 * 职责：承载 `POST /api/auth/email_codes` 的最小输入。
 * 边界：声明邮箱和用途；修改邮箱的账号绑定从认证上下文取得，不接受客户端账号 ID。
 */
public record SendEmailCodeRequest(
        @Schema(description = "目标邮箱", example = "user@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        @Size(max = 320, message = "email length must be less than or equal to 320")
        String email,
        @Schema(description = "验证码用途；CHANGE_EMAIL 要求已登录", example = "LOGIN")
        @NotNull(message = "purpose must not be null")
        EmailCodePurpose purpose
) {
}

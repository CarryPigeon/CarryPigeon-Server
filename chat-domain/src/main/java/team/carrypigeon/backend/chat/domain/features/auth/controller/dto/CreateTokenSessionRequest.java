package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建会话并签发令牌请求。
 * 职责：承载 `POST /api/auth/tokens` 的最小输入。
 * 边界：当前仅支持 `email_code` 授权类型。
 */
public record CreateTokenSessionRequest(
        @Schema(description = "授权类型", example = "email_code")
        @NotBlank(message = "grant_type must not be blank")
        String grantType,
        @Schema(description = "目标邮箱", example = "user@example.com")
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email must not be blank")
        @Size(max = 320, message = "email length must be less than or equal to 320")
        String email,
        @Schema(description = "邮箱验证码", example = "123456")
        @NotBlank(message = "code must not be blank")
        @Pattern(
            regexp = "^[0-9]{6}$",
            message = "code must be a 6-digit number"
        )
        String code
) {}

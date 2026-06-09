package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户名密码登录请求。
 * 职责：承载 `POST /api/auth/login` 的最小输入。
 * 边界：只负责协议层基础校验，不承载认证业务逻辑。
 */
public record LoginRequest(
        @Schema(description = "用户名", example = "carry-user")
        @NotBlank(message = "username must not be blank")
        String username,
        @Schema(description = "密码", example = "password123")
        @NotBlank(message = "password must not be blank")
        String password
) {
}

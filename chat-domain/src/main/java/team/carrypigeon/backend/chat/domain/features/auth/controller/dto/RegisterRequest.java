package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        String password
) {
}

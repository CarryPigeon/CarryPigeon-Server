package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 * 职责：承载 HTTP 登录入口的最小输入校验约束。
 * 边界：不包含验证码、客户端信息或令牌刷新字段。
 *
 * @param username 登录用户名
 * @param password 登录密码
 */
public record LoginRequest(
        @NotBlank(message = "username must not be blank")
        String username,
        @NotBlank(message = "password must not be blank")
        String password
) {
}

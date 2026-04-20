package team.carrypigeon.backend.chat.domain.features.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 * 职责：承载 HTTP 注册入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载注册业务编排。
 *
 * @param username 待注册用户名
 * @param password 待注册密码
 */
public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        @Size(min = 3, max = 32, message = "username length must be between 3 and 32")
        String username,
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 128, message = "password length must be between 8 and 128")
        String password
) {
}

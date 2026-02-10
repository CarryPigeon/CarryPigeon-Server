package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 邮箱验证码发送请求体。
 *
 * @param email 邮箱地址。
 */
public record SendEmailCodeRequest(@NotBlank @Email String email) {
}

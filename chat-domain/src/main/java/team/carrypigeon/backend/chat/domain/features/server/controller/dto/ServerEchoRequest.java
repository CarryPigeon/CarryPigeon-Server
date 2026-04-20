package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP 回显请求。
 * 职责：验证统一响应与参数校验链路是否生效。
 * 边界：当前阶段只用于协议层骨架验证，不承载真实聊天消息语义。
 *
 * @param content 回显内容
 */
public record ServerEchoRequest(
        @NotBlank(message = "content must not be blank")
        String content
) {
}

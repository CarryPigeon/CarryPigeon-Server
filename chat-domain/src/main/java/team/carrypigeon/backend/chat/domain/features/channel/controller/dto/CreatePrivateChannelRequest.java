package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建私有频道请求。
 * 职责：承载私有频道创建 HTTP 入口的最小输入校验约束。
 * 边界：这里只负责协议层输入合法性，不承载创建业务编排。
 *
 * @param name 频道名称
 */
public record CreatePrivateChannelRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name length must be less than or equal to 128")
        String name
) {
}

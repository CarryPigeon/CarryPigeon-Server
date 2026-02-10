package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 频道创建请求体。
 *
 * @param name 频道名称。
 * @param brief 频道简介。
 * @param avatar 频道头像资源键。
 */
public record ChannelCreateRequest(@NotBlank String name,
                                   String brief,
                                   String avatar) {
}

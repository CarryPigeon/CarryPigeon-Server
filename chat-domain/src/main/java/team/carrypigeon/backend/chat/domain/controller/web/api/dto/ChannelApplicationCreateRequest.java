package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 入群申请请求体。
 *
 * @param reason 申请理由。
 */
public record ChannelApplicationCreateRequest(@NotBlank String reason) {
}

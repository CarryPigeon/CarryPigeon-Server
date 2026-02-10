package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 频道禁言设置请求体。
 *
 * @param until 禁言截止时间（毫秒时间戳）。
 * @param reason 禁言原因。
 */
public record ChannelBanUpsertRequest(@NotNull Long until, String reason) {
}

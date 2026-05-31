package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * v1 频道禁言请求。
 */
public record BanChannelMemberV1Request(
        @Schema(description = "禁言截止时间（epoch 毫秒）", example = "1700003600000")
        @Positive(message = "until must be greater than 0")
        Long until,
        @Schema(description = "禁言原因", example = "spam")
        @Size(max = 256, message = "reason length must be less than or equal to 256")
        String reason
) {
}

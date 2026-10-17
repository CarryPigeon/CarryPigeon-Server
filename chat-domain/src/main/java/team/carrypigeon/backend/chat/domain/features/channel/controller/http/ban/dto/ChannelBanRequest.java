package team.carrypigeon.backend.chat.domain.features.channel.controller.http.ban.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 频道禁言请求。
 * 职责：承载禁言截止时间与禁言原因字段。
 * 边界：只做字段承载与协议校验，不表达禁言时长换算、目标角色与治理权限规则。
 */
public record ChannelBanRequest(
        @Schema(description = "禁言截止时间（epoch 毫秒）", example = "1700003600000")
        @Positive(message = "until must be greater than 0")
        Long until,
        @Schema(description = "禁言原因", example = "spam")
        @Size(max = 256, message = "reason length must be less than or equal to 256")
        String reason
) {
}

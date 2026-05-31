package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 审批入群申请请求。
 */
public record DecideChannelApplicationRequest(
        @Schema(description = "审批决定", example = "approve")
        @NotBlank(message = "decision must not be blank")
        String decision
) {
}

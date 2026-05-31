package team.carrypigeon.backend.chat.domain.features.channel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 创建入群申请请求。
 */
public record CreateChannelApplicationRequest(
        @Schema(description = "申请理由", example = "hi")
        @NotNull(message = "reason must not be null")
        String reason
) {
}

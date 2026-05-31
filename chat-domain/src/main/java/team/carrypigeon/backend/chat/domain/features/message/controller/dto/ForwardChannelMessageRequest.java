package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 转发消息请求。
 */
public record ForwardChannelMessageRequest(
        @Schema(description = "目标频道 ID", example = "12345")
        @NotBlank(message = "target_cid must not be blank")
        String targetCid,
        @Schema(description = "附言", example = "FYI")
        String comment,
        @Schema(description = "幂等键", example = "")
        String idempotencyKey
) {
}

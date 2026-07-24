package team.carrypigeon.backend.chat.domain.features.message.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 转发消息请求。
 */
public record ForwardChannelMessageRequest(
        @Schema(description = "目标频道 ID", example = "12345")
        @NotBlank(message = "target_cid must not be blank")
        String targetCid,
        @Schema(description = "附言", example = "FYI")
        String comment,
        @Schema(description = "合并转发源消息 ID，至少两项")
        List<String> mergedMids,
        @Schema(description = "幂等键", example = "forward-001")
        @Size(max = 128, message = "idempotency_key length must be less than or equal to 128")
        String idempotencyKey
) {
}

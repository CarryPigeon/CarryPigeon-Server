package team.carrypigeon.backend.chat.domain.features.plugin.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Domain 约束响应。
 */
public record DomainConstraintsResponse(
        @Schema(description = "最大 payload 字节数", example = "4096")
        int maxPayloadBytes,
        @Schema(description = "最大嵌套深度", example = "10")
        int maxDepth
) {
}

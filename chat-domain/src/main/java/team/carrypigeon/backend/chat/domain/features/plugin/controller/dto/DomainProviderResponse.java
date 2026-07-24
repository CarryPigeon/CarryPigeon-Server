package team.carrypigeon.backend.chat.domain.features.plugin.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Domain 提供方响应。
 */
public record DomainProviderResponse(
        @Schema(description = "提供方类型", example = "core")
        String type,
        @Schema(description = "插件标识", example = "text")
        String pluginId,
        @Schema(description = "最小插件版本", example = "1.0.0")
        String minPluginVersion
) {
}

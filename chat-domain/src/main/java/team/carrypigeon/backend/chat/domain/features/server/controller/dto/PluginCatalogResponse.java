package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 插件目录响应。
 */
public record PluginCatalogResponse(
        @Schema(description = "required_plugins 列表")
        List<String> requiredPlugins,
        @Schema(description = "插件列表")
        List<PluginCatalogItemResponse> plugins
) {
}

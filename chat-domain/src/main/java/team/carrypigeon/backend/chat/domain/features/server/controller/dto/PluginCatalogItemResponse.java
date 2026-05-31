package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 插件目录项。
 */
public record PluginCatalogItemResponse(
        @Schema(description = "插件标识", example = "text")
        String pluginId,
        @Schema(description = "插件名称", example = "Built-in text channel message plugin")
        String name,
        @Schema(description = "版本", example = "1.0.0")
        String version,
        @Schema(description = "最小主机版本", example = "0.1.0")
        String minHostVersion,
        @Schema(description = "是否必需", example = "false")
        boolean required,
        @Schema(description = "权限列表")
        List<String> permissions,
        @Schema(description = "提供的 domain 列表")
        List<PluginDomainResponse> providesDomains,
        @Schema(description = "下载信息")
        PluginDownloadResponse download
) {
}

package team.carrypigeon.backend.chat.domain.features.plugin.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 插件下载信息。
 */
public record PluginDownloadResponse(
        @Schema(description = "下载 URL", example = "https://example.com/plugins/math-formula-1.2.0.zip")
        String url,
        @Schema(description = "校验摘要", example = "")
        String sha256
) {
}

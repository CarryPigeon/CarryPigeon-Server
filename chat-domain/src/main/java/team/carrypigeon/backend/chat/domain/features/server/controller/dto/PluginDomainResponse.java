package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 插件能力域映射。
 */
public record PluginDomainResponse(
        @Schema(description = "domain 名称", example = "Core:Text")
        String domain,
        @Schema(description = "domain 版本", example = "1.0.0")
        String domainVersion
) {
}

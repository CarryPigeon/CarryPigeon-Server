package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Domain 目录项。
 */
public record DomainCatalogItemResponse(
        @Schema(description = "domain 名称", example = "Core:Text")
        String domain,
        @Schema(description = "支持版本列表")
        List<String> supportedVersions,
        @Schema(description = "推荐版本", example = "1.0.0")
        String recommendedVersion,
        @Schema(description = "约束信息")
        DomainConstraintsResponse constraints,
        @Schema(description = "提供方列表")
        List<DomainProviderResponse> providers
) {
}

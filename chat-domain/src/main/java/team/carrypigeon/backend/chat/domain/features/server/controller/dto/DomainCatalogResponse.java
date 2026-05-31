package team.carrypigeon.backend.chat.domain.features.server.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Domain 目录响应。
 */
public record DomainCatalogResponse(
        @Schema(description = "domain 列表")
        List<DomainCatalogItemResponse> items
) {
}

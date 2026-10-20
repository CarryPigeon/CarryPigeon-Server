package team.carrypigeon.backend.chat.domain.features.server.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.result.ServerCapabilities;

/**
 * 服务端能力响应。
 * 职责：向客户端声明 discovery 阶段已稳定支持的能力开关。
 * 边界：只表达最小布尔能力，不扩展业务端点细节。
 */
public record ServerCapabilitiesResponse(
        @Schema(description = "是否支持消息 domain 能力", example = "true") boolean messageDomains,
        @Schema(description = "是否支持插件目录能力", example = "false") boolean pluginCatalog,
        @Schema(description = "是否支持事件恢复能力", example = "false") boolean eventResume
) {

    /**
     * 从领域能力摘要构造响应。
     *
     * @param capabilities 领域能力摘要
     * @return 对应响应
     */
    public static ServerCapabilitiesResponse from(ServerCapabilities capabilities) {
        return new ServerCapabilitiesResponse(
                capabilities.messageDomains(),
                capabilities.pluginCatalog(),
                capabilities.eventResume()
        );
    }
}

package team.carrypigeon.backend.chat.domain.features.server.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 服务端能力摘要。
 * 职责：向客户端声明当前公开协议面已经稳定支持的关键能力开关。
 * 边界：这里只表达 discovery 阶段需要的最小布尔能力，不扩展业务端点细节。
 */
public record ServerCapabilities(
        @Schema(description = "是否支持消息 domain 能力", example = "true")
        boolean messageDomains,
        @Schema(description = "是否支持插件目录能力", example = "false")
        boolean pluginCatalog,
        @Schema(description = "是否支持事件恢复能力", example = "false")
        boolean eventResume
) {
}

package team.carrypigeon.backend.chat.domain.features.server.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * `/api/server` 公开发现文档。
 * 职责：为客户端握手与登录前置阶段提供最小发现信息。
 * 边界：只承载当前 v1 已落地的公开入口字段，不混入主业务资源数据。
 */
public record ServerDiscoveryDocument(
        @Schema(description = "当前服务端稳定 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String serverId,
        @Schema(description = "服务端公开名称", example = "CarryPigeon Server")
        String name,
        @Schema(description = "服务端公开简介", example = "A self-hosted chat server")
        String brief,
        @Schema(description = "服务端头像相对路径", example = "api/files/download/server_avatar")
        String avatar,
        @Schema(description = "当前 API 版本", example = "1.0")
        String apiVersion,
        @Schema(description = "最小支持 API 版本", example = "1.0")
        String minSupportedApiVersion,
        @Schema(description = "实时通道地址", example = "wss://example.com/api/ws")
        String wsUrl,
        @Schema(description = "required gate 插件列表")
        List<String> requiredPlugins,
        @Schema(description = "公开能力开关")
        ServerCapabilities capabilities,
        @Schema(description = "服务端当前时间（epoch 毫秒）", example = "1700000000000")
        long serverTime
) {
}

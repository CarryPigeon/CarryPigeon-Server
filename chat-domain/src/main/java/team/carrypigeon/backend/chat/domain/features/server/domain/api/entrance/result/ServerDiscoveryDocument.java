package team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.result;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * `/api/server` 公开发现文档。
 * 职责：为客户端握手与登录前置阶段提供最小发现信息。
 * 边界：只承载当前已落地的公开入口字段，不混入主业务资源数据。
 */
public record ServerDiscoveryDocument(
        @Schema(description = "当前服务端稳定 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String serverId,
        @Schema(description = "服务端公开名称", example = "CarryPigeon Server")
        String name,
        @Schema(description = "服务端公开简介", example = "A self-hosted chat server")
        String brief,
        @Schema(description = "实时通道地址", example = "ws://127.0.0.1:18080/api/ws")
        String wsUrl,
        @Schema(description = "服务端当前时间（epoch 毫秒）", example = "1700000000000")
        long serverTime
) {
}

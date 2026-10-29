package team.carrypigeon.backend.chat.domain.features.server.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.result.ServerDiscoveryDocument;

/**
 * `/api/server` 公开发现响应。
 * 职责：把领域发现文档转换为客户端握手与登录前置阶段使用的响应结构。
 */
public record ServerDiscoveryResponse(
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

    /**
     * 从领域发现文档构造响应。
     *
     * @param document 领域发现文档
     * @return 对应响应
     */
    public static ServerDiscoveryResponse from(ServerDiscoveryDocument document) {
        return new ServerDiscoveryResponse(
                document.serverId(),
                document.name(),
                document.brief(),
                document.wsUrl(),
                document.serverTime()
        );
    }
}

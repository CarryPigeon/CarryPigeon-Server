package team.carrypigeon.backend.chat.domain.features.server.domain.api.entrance.result;

import team.carrypigeon.backend.chat.domain.features.server.config.PluginPropertyItem;

/**
 * `/api/server` 公开发现文档。
 * 职责：为客户端握手与登录前置阶段提供最小发现信息。
 * 边界：只承载当前已落地的公开入口字段，不混入主业务资源数据。
 */
public record ServerDiscoveryDocument(
        String serverId,
        String name,
        String brief,
        String wsUrl,
        long serverTime,
        PluginPropertyItem[] plugins
) {
}

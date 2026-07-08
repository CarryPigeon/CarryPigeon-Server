package team.carrypigeon.backend.chat.domain.features.server.domain.service;

/**
 * 实时服务发现设置。
 * 职责：为服务发现领域服务提供最小 realtime 公开信息。
 * 边界：不承载 Netty 运行时、线程模型或配置绑定细节。
 */
public record RealtimeDiscoverySettings(boolean enabled, String host, int port, String path) {

    /**
     * 返回服务发现可公开的 websocket 地址。
     *
     * @return realtime 启用时返回 ws 地址；禁用时返回 null
     */
    public String wsUrl() {
        if (!enabled) {
            return null;
        }
        return "ws://" + host + ":" + port + path;
    }
}

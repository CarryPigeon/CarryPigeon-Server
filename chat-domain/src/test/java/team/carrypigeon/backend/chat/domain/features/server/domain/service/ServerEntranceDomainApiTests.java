package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ServerEntranceDomainApi 契约测试。
 * 职责：验证服务端基础领域服务对公开源信息文档的最小组装逻辑。
 * 边界：不验证 HTTP 协议层，只验证应用层输出字段。
 */
@Tag("contract")
class ServerEntranceDomainApiTests {

    /**
     * 验证服务发现文档会输出当前连接所需的最小公开信息。
     */
    @Test
    @DisplayName("get server discovery document returns minimal public metadata")
    void getServerDiscoveryDocument_returnsMinimalPublicMetadata() {
        ServerEntranceDomainApi service = new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                realtimeProperties(true),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("mc-bind")
        );

        var result = service.getServerDiscoveryDocument();

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.serverId());
        assertEquals("CarryPigeonBackend", result.name());
        assertEquals("A self-hosted chat server", result.brief());
        assertEquals("/api/files/download/server_avatar", result.avatar());
        assertEquals("1.0", result.apiVersion());
        assertEquals("1.0", result.minSupportedApiVersion());
        assertEquals("ws://127.0.0.1:28080/api/ws", result.wsUrl());
        assertEquals(java.util.List.of("mc-bind"), result.requiredPlugins());
        assertEquals(1713614400000L, result.serverTime());
        assertEquals(true, result.capabilities().eventResume());
    }

    /**
     * 验证 `getServerDiscoveryDocument` 在 `realtimeDisabled` 条件下满足 `hidesWebsocketUrl` 的测试契约。
     */
    @Test
    @DisplayName("get server discovery document realtime disabled hides websocket url")
    void getServerDiscoveryDocument_realtimeDisabled_hidesWebsocketUrl() {
        ServerEntranceDomainApi service = new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                realtimeProperties(false),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of()
        );

        var result = service.getServerDiscoveryDocument();

        assertEquals(null, result.wsUrl());
        assertEquals(false, result.capabilities().eventResume());
    }

    /**
     * 验证 required gate 缺失插件会被正确识别。
     */
    @Test
    @DisplayName("find missing required plugins returns plugins not installed")
    void findMissingRequiredPlugins_returnsPluginsNotInstalled() {
        ServerEntranceDomainApi service = new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                realtimeProperties(true),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("mc-bind", "math-formula")
        );

        var result = service.findMissingRequiredPlugins(java.util.List.of("mc-bind"));

        assertEquals(java.util.List.of("math-formula"), result);
    }

    /**
     * 验证 required plugin 配置会忽略空条目并裁剪空白，避免空配置误触发 required gate。
     */
    @Test
    @DisplayName("required plugins blank entries are ignored")
    void requiredPlugins_blankEntries_areIgnored() {
        ServerEntranceDomainApi service = new ServerEntranceDomainApi(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                realtimeProperties(true),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("", "  mc-bind  ", " ")
        );

        var discovery = service.getServerDiscoveryDocument();
        var missingPlugins = service.findMissingRequiredPlugins(java.util.List.of("mc-bind"));

        assertEquals(java.util.List.of("mc-bind"), discovery.requiredPlugins());
        assertEquals(java.util.List.of(), missingPlugins);
    }

    private RealtimeDiscoverySettings realtimeProperties(boolean enabled) {
        return new RealtimeDiscoverySettings(enabled, "127.0.0.1", 28080, "/api/ws");
    }
}

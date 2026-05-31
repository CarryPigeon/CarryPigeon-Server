package team.carrypigeon.backend.chat.domain.features.server.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ServerApplicationService 契约测试。
 * 职责：验证服务端基础应用服务对公开源信息文档的最小组装逻辑。
 * 边界：不验证 HTTP 协议层，只验证应用层输出字段。
 */
@Tag("contract")
class ServerApplicationServiceTests {

    /**
     * 验证服务发现文档会输出当前连接所需的最小公开信息。
     */
    @Test
    @DisplayName("get server discovery document returns minimal public metadata")
    void getServerDiscoveryDocument_returnsMinimalPublicMetadata() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(
                        registration("builtin-voice-message", "voice", "voice", true),
                        registration("builtin-file-message", "file", "file", true),
                        registration("builtin-custom-message", "custom", "custom", true),
                        registration("builtin-text-message", "text", "text", true)
                )),
                realtimeProperties(true),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("mc-bind")
        );

        var result = service.getServerDiscoveryDocument();

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.serverId());
        assertEquals("CarryPigeonBackend", result.name());
        assertEquals("A self-hosted chat server", result.brief());
        assertEquals("api/files/download/server_avatar", result.avatar());
        assertEquals("1.0", result.apiVersion());
        assertEquals("1.0", result.minSupportedApiVersion());
        assertEquals("wss://127.0.0.1:28080/api/ws", result.wsUrl());
        assertEquals(java.util.List.of("mc-bind"), result.requiredPlugins());
        assertEquals(1713614400000L, result.serverTime());
    }

    /**
     * 验证 required gate 缺失插件会被正确识别。
     */
    @Test
    @DisplayName("find missing required plugins returns plugins not installed")
    void findMissingRequiredPlugins_returnsPluginsNotInstalled() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(
                        registration("builtin-text-message", "text", "text", true),
                        registration("builtin-custom-message", "custom", "custom", false)
                )),
                realtimeProperties(true),
                new TimeProvider(Clock.fixed(Instant.parse("2024-04-20T12:00:00Z"), ZoneOffset.UTC)),
                java.util.List.of("mc-bind", "math-formula")
        );

        var result = service.findMissingRequiredPlugins(java.util.List.of("mc-bind"));

        assertEquals(java.util.List.of("math-formula"), result);
    }

    private ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            boolean publicVisible
    ) {
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        publicPluginKey,
                        "test plugin",
                        publicVisible,
                        java.util.List.of("message.sent"),
                        java.util.List.of("message:" + messageType + ":send"),
                        "test_condition"
                ),
                new TextChannelMessagePlugin() {
                    @Override
                    public String supportedType() {
                        return messageType;
                    }
                }
        );
    }

    private RealtimeServerProperties realtimeProperties(boolean enabled) {
        return new RealtimeServerProperties(enabled, "127.0.0.1", 28080, "/api/ws", 1, 0);
    }
}

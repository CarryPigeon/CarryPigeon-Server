package team.carrypigeon.backend.chat.domain.features.server.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.CurrentPresenceStatus;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.WellKnownServerDocument;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import io.netty.channel.embedded.EmbeddedChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerApplicationService 契约测试。
 * 职责：验证服务端基础应用服务对公开源信息文档的最小组装逻辑。
 * 边界：不验证 HTTP 协议层，只验证应用层输出字段。
 */
@Tag("contract")
class ServerApplicationServiceTests {

    /**
     * 验证公开源信息文档会使用当前 serverId、应用名和最小登录方式集合。
     */
    @Test
    @DisplayName("get well known server document returns minimal public metadata")
    void getWellKnownServerDocument_returnsMinimalPublicMetadata() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(
                        registration("builtin-voice-message", "voice", "voice", true),
                        registration("builtin-file-message", "file", "file", true),
                        registration("builtin-custom-message", "custom", "custom", true),
                        registration("builtin-plugin-message", "plugin", "plugin", true),
                        registration("builtin-text-message", "text", "text", true)
                )),
                realtimeProperties(true),
                new RealtimeSessionRegistry()
        );

        WellKnownServerDocument result = service.getWellKnownServerDocument();

        assertEquals("carrypigeon-local", result.serverId());
        assertEquals("CarryPigeonBackend", result.serverName());
        assertTrue(result.registerEnabled());
        assertEquals(java.util.List.of("username_password"), result.loginMethods());
        assertEquals(java.util.List.of("user_registration", "username_password_login"), result.publicCapabilities());
        assertEquals(java.util.List.of("custom", "file", "plugin", "text", "voice"), result.publicPlugins());
    }

    /**
     * 验证 registry 未公开的插件不会进入 well-known public_plugins。
     */
    @Test
    @DisplayName("get well known server document excludes disabled public plugins")
    void getWellKnownServerDocument_excludesDisabledPublicPlugins() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(
                        registration("builtin-text-message", "text", "text", true),
                        registration("builtin-plugin-message", "plugin", "plugin", false)
                )),
                realtimeProperties(true),
                new RealtimeSessionRegistry()
        );

        WellKnownServerDocument result = service.getWellKnownServerDocument();

        assertEquals(java.util.List.of("text"), result.publicPlugins());
    }

    /**
     * 验证启用 realtime 且存在活跃会话时返回 ONLINE。
     */
    @Test
    @DisplayName("get current presence realtime enabled and online returns online")
    void getCurrentPresence_realtimeEnabledAndOnline_returnsOnline() {
        RealtimeSessionRegistry registry = new RealtimeSessionRegistry();
        EmbeddedChannel channel = new EmbeddedChannel();
        registry.register(1001L, channel);
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(registration("builtin-text-message", "text", "text", true))),
                realtimeProperties(true),
                registry
        );

        var result = service.getCurrentPresence(1001L);

        assertEquals(CurrentPresenceStatus.ONLINE, result.status());
        assertEquals(1, result.onlineSessionCount());
    }

    /**
     * 验证启用 realtime 但无活跃会话时返回 OFFLINE。
     */
    @Test
    @DisplayName("get current presence realtime enabled and offline returns offline")
    void getCurrentPresence_realtimeEnabledAndOffline_returnsOffline() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(registration("builtin-text-message", "text", "text", true))),
                realtimeProperties(true),
                new RealtimeSessionRegistry()
        );

        var result = service.getCurrentPresence(1001L);

        assertEquals(CurrentPresenceStatus.OFFLINE, result.status());
        assertEquals(0, result.onlineSessionCount());
    }

    /**
     * 验证关闭 realtime 时返回 UNAVAILABLE，而不是误判为离线。
     */
    @Test
    @DisplayName("get current presence realtime disabled returns unavailable")
    void getCurrentPresence_realtimeDisabled_returnsUnavailable() {
        ServerApplicationService service = new ServerApplicationService(
                new ServerIdentityProperties("carrypigeon-local"),
                "CarryPigeonBackend",
                new ChannelMessagePluginRegistry(java.util.List.of(registration("builtin-text-message", "text", "text", true))),
                realtimeProperties(false),
                new RealtimeSessionRegistry()
        );

        var result = service.getCurrentPresence(1001L);

        assertEquals(CurrentPresenceStatus.UNAVAILABLE, result.status());
        assertEquals(0, result.onlineSessionCount());
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
        return new RealtimeServerProperties(enabled, "127.0.0.1", 28080, "/ws", 1, 0);
    }
}

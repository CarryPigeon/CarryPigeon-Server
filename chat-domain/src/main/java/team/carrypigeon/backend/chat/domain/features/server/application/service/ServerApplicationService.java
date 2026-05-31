package team.carrypigeon.backend.chat.domain.features.server.application.service;

import java.util.List;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.RequiredGateCheckResult;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerCapabilities;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 服务基础应用服务。
 * 职责：为协议层提供当前服务入口的最小用例编排与公开源信息组装。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerApplicationService {

    private static final String DEFAULT_BRIEF = "A self-hosted chat server";
    private static final String DEFAULT_AVATAR = "api/files/download/server_avatar";
    private static final String API_VERSION = "1.0";

    private final ServerIdentityProperties serverIdentityProperties;
    private final String applicationName;
    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    private final RealtimeServerProperties realtimeServerProperties;
    private final TimeProvider timeProvider;
    private final List<String> requiredPlugins;

    public ServerApplicationService(
            ServerIdentityProperties serverIdentityProperties,
            @Value("${spring.application.name:CarryPigeonBackend}") String applicationName,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            RealtimeServerProperties realtimeServerProperties,
            TimeProvider timeProvider,
            @Value("${cp.chat.server.required-plugins:}") List<String> requiredPlugins
    ) {
        this.serverIdentityProperties = serverIdentityProperties;
        this.applicationName = applicationName;
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.realtimeServerProperties = realtimeServerProperties;
        this.timeProvider = timeProvider;
        this.requiredPlugins = requiredPlugins == null ? List.of() : List.copyOf(requiredPlugins);
    }

    public ServerApplicationService(
            ServerIdentityProperties serverIdentityProperties,
            String applicationName,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            RealtimeServerProperties realtimeServerProperties
    ) {
        this(
                serverIdentityProperties,
                applicationName,
                channelMessagePluginRegistry,
                realtimeServerProperties,
                new TimeProvider(Clock.systemUTC()),
                List.of()
        );
    }

    /**
     * 返回 v1 服务发现文档。
     *
     * @return 客户端握手阶段可直接消费的发现信息
     */
    public ServerDiscoveryDocument getServerDiscoveryDocument() {
        return new ServerDiscoveryDocument(
                serverIdentityProperties.id(),
                applicationName,
                DEFAULT_BRIEF,
                DEFAULT_AVATAR,
                API_VERSION,
                API_VERSION,
                resolveWsUrl(),
                requiredPlugins,
                new ServerCapabilities(true, true, true),
                timeProvider.nowMillis()
        );
    }

    /**
     * 返回当前设备缺失的 required 插件列表。
     *
     * @param installedPluginIds 客户端已安装插件 ID
     * @return 当前缺失的必需插件列表
     */
    public List<String> findMissingRequiredPlugins(List<String> installedPluginIds) {
        List<String> installed = installedPluginIds == null ? List.of() : installedPluginIds;
        return requiredPlugins.stream().filter(requiredPlugin -> !installed.contains(requiredPlugin)).toList();
    }

    private String resolveWsUrl() {
        return "wss://" + realtimeServerProperties.host() + ":" + realtimeServerProperties.port() + realtimeServerProperties.path();
    }
}

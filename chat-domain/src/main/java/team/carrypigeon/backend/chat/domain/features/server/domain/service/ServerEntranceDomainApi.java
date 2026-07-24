package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerCapabilities;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginCatalogApi;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 服务基础领域服务。
 * 职责：为协议层提供当前服务入口的最小用例编排与公开源信息组装。
 * 边界：当前阶段只返回协议骨架状态，不承载具体聊天业务逻辑。
 */
@Service
public class ServerEntranceDomainApi implements ServerEntranceApi {

    private static final String DEFAULT_BRIEF = "A self-hosted chat server";
    private static final String DEFAULT_AVATAR = "/api/files/download/server_avatar";
    private static final String API_VERSION = "1.0";

    private final ServerIdentityProvider serverIdentityProvider;
    private final String applicationName;
    private final RealtimeDiscoverySettings realtimeDiscoverySettings;
    private final TimeProvider timeProvider;
    private final PluginCatalogApi pluginCatalogApi;

    @Autowired
    public ServerEntranceDomainApi(
            ServerIdentityProvider serverIdentityProvider,
            @Value("${spring.application.name:CarryPigeonBackend}") String applicationName,
            RealtimeDiscoverySettings realtimeDiscoverySettings,
            TimeProvider timeProvider,
            PluginCatalogApi pluginCatalogApi
    ) {
        this.serverIdentityProvider = serverIdentityProvider;
        this.applicationName = applicationName;
        this.realtimeDiscoverySettings = realtimeDiscoverySettings;
        this.timeProvider = timeProvider;
        this.pluginCatalogApi = pluginCatalogApi;
    }

    /**
     * 返回 v1 服务发现文档。
     *
     * @return 客户端握手阶段可直接消费的发现信息
     */
    public ServerDiscoveryDocument getServerDiscoveryDocument() {
        return new ServerDiscoveryDocument(
                serverIdentityProvider.id(),
                applicationName,
                DEFAULT_BRIEF,
                DEFAULT_AVATAR,
                API_VERSION,
                API_VERSION,
                realtimeDiscoverySettings.wsUrl(),
                pluginCatalogApi.requiredPluginIds(),
                new ServerCapabilities(true, true, realtimeDiscoverySettings.enabled()),
                timeProvider.nowMillis()
        );
    }

}

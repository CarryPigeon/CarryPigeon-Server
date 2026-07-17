package team.carrypigeon.backend.chat.domain.features.server.domain.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerCapabilities;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
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
    private final List<String> requiredPlugins;

    @Autowired
    public ServerEntranceDomainApi(
            ServerIdentityProvider serverIdentityProvider,
            @Value("${spring.application.name:CarryPigeonBackend}") String applicationName,
            RealtimeDiscoverySettings realtimeDiscoverySettings,
            TimeProvider timeProvider,
            @Value("${cp.chat.server.required-plugins:}") List<String> requiredPlugins
    ) {
        this.serverIdentityProvider = serverIdentityProvider;
        this.applicationName = applicationName;
        this.realtimeDiscoverySettings = realtimeDiscoverySettings;
        this.timeProvider = timeProvider;
        this.requiredPlugins = normalizeRequiredPlugins(requiredPlugins);
    }

    public ServerEntranceDomainApi(
            ServerIdentityProvider serverIdentityProvider,
            String applicationName,
            RealtimeDiscoverySettings realtimeDiscoverySettings
    ) {
        this(
                serverIdentityProvider,
                applicationName,
                realtimeDiscoverySettings,
                new TimeProvider(Clock.systemUTC()),
                List.of()
        );
    }

    /**
     * 规范化 required plugin 配置。
     * 约束：空配置项不参与客户端准入校验，避免配置中的空白项被误判为必需插件。
     *
     * @param requiredPlugins 原始配置插件 ID 列表
     * @return 去空白、去重后的 required plugin 列表
     */
    private static List<String> normalizeRequiredPlugins(List<String> requiredPlugins) {
        if (requiredPlugins == null) {
            return List.of();
        }
        return requiredPlugins.stream()
                .filter(pluginId -> pluginId != null && !pluginId.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
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
                requiredPlugins,
                new ServerCapabilities(true, true, realtimeDiscoverySettings.enabled()),
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

    /**
     * 要求客户端满足 required plugin gate。
     *
     * @param installedPluginIds 客户端已安装插件 ID
     */
    @Override
    public void requireRequiredPluginsSatisfied(List<String> installedPluginIds) {
        List<String> missingPlugins = findMissingRequiredPlugins(installedPluginIds);
        if (!missingPlugins.isEmpty()) {
            throw ProblemException.validationFailed(
                    "required_plugin_missing",
                    "required plugins are missing",
                    Map.of("missing_plugins", missingPlugins)
            );
        }
    }
}

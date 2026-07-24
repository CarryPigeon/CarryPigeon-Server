package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginCatalogApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.projection.PluginCatalogItemResult;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息插件目录领域服务。
 * 职责：为其它 feature 提供稳定的消息插件目录查询入口。
 * 边界：只暴露目录快照与能力判断，不泄露 message feature 的 support 实现细节。
 */
@Service
public class PluginCatalogDomainApi implements PluginCatalogApi {

    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    private final List<String> requiredPluginIds;

    @Autowired
    public PluginCatalogDomainApi(
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            @Value("${cp.chat.server.required-plugins:}") List<String> requiredPluginIds
    ) {
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.requiredPluginIds = normalizeRequiredPlugins(requiredPluginIds);
    }

    public PluginCatalogDomainApi(ChannelMessagePluginRegistry channelMessagePluginRegistry) {
        this(channelMessagePluginRegistry, List.of());
    }

    /**
     * 返回当前运行时公开插件目录。
     *
     * @return 可公开暴露的插件目录项
     */
    public List<PluginCatalogItemResult> listPublicPlugins() {
        return channelMessagePluginRegistry.getDescriptors().stream()
                .filter(descriptor -> descriptor.publicVisible())
                .map(descriptor -> new PluginCatalogItemResult(
                        descriptor.messageType(),
                        channelMessagePluginRegistry.require(descriptor.messageType()).supportedDomain(),
                        descriptor.publicPluginKey(),
                        descriptor.description(),
                        descriptor.declaredPermissions()
                ))
                .toList();
    }

    /**
     * 判断扩展消息类型是否在当前运行时白名单中。
     *
     * @param messageType 扩展消息类型
     * @return 白名单命中时返回 true
     */
    public List<String> requiredPluginIds() {
        return requiredPluginIds;
    }

    @Override
    public List<String> findMissingRequiredPlugins(List<String> installedPluginIds) {
        List<String> installed = installedPluginIds == null ? List.of() : installedPluginIds;
        return requiredPluginIds.stream().filter(requiredPlugin -> !installed.contains(requiredPlugin)).toList();
    }

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

    private static List<String> normalizeRequiredPlugins(List<String> requiredPluginIds) {
        if (requiredPluginIds == null) {
            return List.of();
        }
        return requiredPluginIds.stream()
                .filter(pluginId -> pluginId != null && !pluginId.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}

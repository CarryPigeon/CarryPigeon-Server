package team.carrypigeon.backend.chat.domain.features.plugin.domain.service.catalog;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.catalog.PluginCatalogApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.catalog.result.PluginCatalogItemResult;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.problem.PluginProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageDispatcher;

/**
 * 消息插件目录领域服务。
 * 职责：为其它 feature 提供稳定的消息 domain 目录查询入口。
 * 边界：只暴露当前已注册的 domain 快照与 required plugin 判断，不泄露具体 handler 实现。
 */
@Service
public class PluginCatalogDomainApi implements PluginCatalogApi {

    private static final String BUILT_IN_DESCRIPTION = "Built-in message domain";

    private final CPChannelMessageDispatcher channelMessageDispatcher;
    private final List<String> requiredPluginIds;

    public PluginCatalogDomainApi(
            CPChannelMessageDispatcher channelMessageDispatcher,
            @Value("${cp.chat.server.required-plugins:}") List<String> requiredPluginIds
    ) {
        this.channelMessageDispatcher = channelMessageDispatcher;
        this.requiredPluginIds = normalizeRequiredPlugins(requiredPluginIds);
    }

    /**
     * 返回当前运行时公开的消息 domain 目录。
     *
     * @return 可公开暴露的 domain 目录项
     */
    public List<PluginCatalogItemResult> listPublicPlugins() {
        return channelMessageDispatcher.supportedDomains().stream()
                .map(domain -> new PluginCatalogItemResult(
                        messageTypeOf(domain),
                        domain,
                        domain,
                        BUILT_IN_DESCRIPTION,
                        List.of()
                ))
                .toList();
    }

    /**
     * 返回服务端要求客户端安装的插件 ID。
     *
     * @return 去空白、去重后的 required plugin ID
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
            throw ProblemException.of(
                    PluginProblem.REQUIRED_PLUGIN_MISSING,
                    Map.of("missing_plugins", missingPlugins)
            );
        }
    }

    /**
     * 从 domain 中提取中间段作为展示用消息类型。
     * 约束：domain 由 controller 保证为 `主分类:子分类:版本号` 形式。
     *
     * @param domain 消息 domain
     * @return 展示用消息类型
     */
    private String messageTypeOf(String domain) {
        String[] parts = domain.split(":");
        if (parts.length < 2 || parts[1].isBlank()) {
            return domain;
        }
        return parts[1].toLowerCase(Locale.ROOT);
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

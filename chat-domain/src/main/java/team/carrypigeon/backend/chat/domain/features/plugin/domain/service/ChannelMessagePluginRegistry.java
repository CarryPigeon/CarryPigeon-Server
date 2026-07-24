package team.carrypigeon.backend.chat.domain.features.plugin.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息插件注册器。
 * 职责：管理当前运行时可用的消息插件，并按消息类型分派处理器。
 * 边界：只负责插件注册与查找，不承载消息业务编排。
 */
public class ChannelMessagePluginRegistry {

    private static final java.util.Set<String> BUILTIN_MESSAGE_TYPES = java.util.Set.of("text", "file", "voice");

    private final Map<String, ChannelMessagePlugin> pluginsByType;
    private final Map<String, ChannelMessagePlugin> pluginsByDomain;
    private final Map<String, ChannelMessagePluginDescriptor> descriptorsByType;
    private final Map<String, String> messageTypesByPublicPluginKey;

    public ChannelMessagePluginRegistry(List<ChannelMessagePluginRegistration> registrations) {
        Map<String, ChannelMessagePlugin> resolvedPlugins = new LinkedHashMap<>();
        Map<String, ChannelMessagePlugin> resolvedDomainPlugins = new LinkedHashMap<>();
        Map<String, ChannelMessagePluginDescriptor> resolvedDescriptors = new LinkedHashMap<>();
        Map<String, String> publicPluginKeys = new LinkedHashMap<>();
        for (ChannelMessagePluginRegistration registration : registrations) {
            ChannelMessagePlugin plugin = registration.plugin();
            ChannelMessagePluginDescriptor descriptor = registration.descriptor();
            ChannelMessagePlugin previous = resolvedPlugins.putIfAbsent(plugin.supportedType(), plugin);
            if (previous != null) {
                throw new IllegalStateException("duplicate channel message plugin type: " + plugin.supportedType());
            }
            ChannelMessagePlugin previousDomain = resolvedDomainPlugins.putIfAbsent(plugin.supportedDomain(), plugin);
            if (previousDomain != null) {
                throw new IllegalStateException("duplicate channel message plugin domain: " + plugin.supportedDomain());
            }
            resolvedDescriptors.put(plugin.supportedType(), descriptor);
            if (descriptor.publicVisible()) {
                String previousType = publicPluginKeys.putIfAbsent(descriptor.publicPluginKey(), descriptor.messageType());
                if (previousType != null) {
                    throw new IllegalStateException("duplicate public plugin key: " + descriptor.publicPluginKey());
                }
            }
        }
        this.pluginsByType = Map.copyOf(resolvedPlugins);
        this.pluginsByDomain = Map.copyOf(resolvedDomainPlugins);
        this.descriptorsByType = Map.copyOf(resolvedDescriptors);
        this.messageTypesByPublicPluginKey = Map.copyOf(publicPluginKeys);
    }

    /**
     * 按消息类型获取插件。
     *
     * @param messageType 消息类型
     * @return 对应插件
     */
    public ChannelMessagePlugin require(String messageType) {
        ChannelMessagePlugin plugin = pluginsByType.get(messageType);
        if (plugin == null) {
            throw ProblemException.validationFailed("unsupported message type");
        }
        return plugin;
    }

    /**
     * 按对外 domain 获取负责 data 校验的插件。
     *
     * @param domain canonical 消息 domain
     * @return 对应插件
     */
    public ChannelMessagePlugin requireDomain(String domain) {
        ChannelMessagePlugin plugin = pluginsByDomain.get(domain);
        if (plugin == null) {
            throw ProblemException.validationFailed("schema_invalid", "domain is not supported");
        }
        return plugin;
    }

    /**
     * 返回当前运行时可公开暴露的插件标识列表。
     *
     * @return 稳定 public_plugins 列表
     */
    public List<String> getPublicPluginKeys() {
        return descriptorsByType.values().stream()
                .filter(ChannelMessagePluginDescriptor::publicVisible)
                .map(ChannelMessagePluginDescriptor::publicPluginKey)
                .sorted()
                .toList();
    }

    /**
     * 返回当前运行时的插件治理描述列表。
     *
     * @return 当前注册插件描述快照
     */
    public List<ChannelMessagePluginDescriptor> getDescriptors() {
        return descriptorsByType.values().stream().toList();
    }

    /**
     * 判断当前消息类型是否已在运行时启用。
     *
     * @param messageType 消息类型
     * @return 已启用时返回 true
     */
    public boolean supports(String messageType) {
        return pluginsByType.containsKey(messageType);
    }

    /**
     * 判断 canonical HTTP domain 是否存在已注册校验插件。
     *
     * @param domain 消息 domain
     * @return 已注册时返回 true
     */
    public boolean supportsDomain(String domain) {
        return pluginsByDomain.containsKey(domain);
    }

    /**
     * 判断扩展消息类型是否在当前运行时白名单中。
     *
     * @param messageType 扩展消息类型
     * @return 白名单命中时返回 true
     */
    public boolean supportsExtensionMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return false;
        }
        String normalizedType = messageType.trim();
        if (BUILTIN_MESSAGE_TYPES.contains(normalizedType)) {
            return false;
        }
        return messageTypesByPublicPluginKey.containsKey(normalizedType);
    }
}

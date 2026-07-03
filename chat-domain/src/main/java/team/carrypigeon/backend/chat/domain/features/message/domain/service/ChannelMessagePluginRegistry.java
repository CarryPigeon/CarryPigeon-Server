package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息插件注册器。
 * 职责：管理当前运行时可用的消息插件，并按消息类型分派处理器。
 * 边界：只负责插件注册与查找，不承载消息业务编排。
 */
public class ChannelMessagePluginRegistry {

    private static final java.util.Set<String> BUILTIN_MESSAGE_TYPES = java.util.Set.of("text", "file", "voice");

    private final Map<String, ChannelMessagePlugin> pluginsByType;
    private final Map<String, ChannelMessagePluginDescriptor> descriptorsByType;
    private final Map<String, String> messageTypesByPublicPluginKey;

    public ChannelMessagePluginRegistry(List<ChannelMessagePluginRegistration> registrations) {
        Map<String, ChannelMessagePlugin> resolvedPlugins = new LinkedHashMap<>();
        Map<String, ChannelMessagePluginDescriptor> resolvedDescriptors = new LinkedHashMap<>();
        Map<String, String> publicPluginKeys = new LinkedHashMap<>();
        for (ChannelMessagePluginRegistration registration : registrations) {
            ChannelMessagePlugin plugin = registration.plugin();
            ChannelMessagePluginDescriptor descriptor = registration.descriptor();
            ChannelMessagePlugin previous = resolvedPlugins.putIfAbsent(plugin.supportedType(), plugin);
            if (previous != null) {
                throw new IllegalStateException("duplicate channel message plugin type: " + plugin.supportedType());
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

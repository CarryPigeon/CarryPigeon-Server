package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 频道消息插件注册器。
 * 职责：管理当前运行时可用的消息插件，并按消息类型分派处理器。
 * 边界：只负责插件注册与查找，不承载消息业务编排。
 */
public class ChannelMessagePluginRegistry {

    private final Map<String, ChannelMessagePlugin> pluginsByType;

    public ChannelMessagePluginRegistry(List<ChannelMessagePlugin> plugins) {
        Map<String, ChannelMessagePlugin> resolvedPlugins = new LinkedHashMap<>();
        for (ChannelMessagePlugin plugin : plugins) {
            ChannelMessagePlugin previous = resolvedPlugins.putIfAbsent(plugin.supportedType(), plugin);
            if (previous != null) {
                throw new IllegalStateException("duplicate channel message plugin type: " + plugin.supportedType());
            }
        }
        this.pluginsByType = Map.copyOf(resolvedPlugins);
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
}

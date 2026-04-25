package team.carrypigeon.backend.chat.domain.features.message.domain.service;

/**
 * 频道消息插件注册项。
 * 职责：把运行时插件实现与对应治理描述绑定为一个稳定注册单元。
 * 边界：这里只保证插件类型与描述一致，不承载注册表查找逻辑。
 *
 * @param descriptor 插件治理描述
 * @param plugin 运行时插件实现
 */
public record ChannelMessagePluginRegistration(
        ChannelMessagePluginDescriptor descriptor,
        ChannelMessagePlugin plugin
) {

    public ChannelMessagePluginRegistration {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        if (!descriptor.messageType().equals(plugin.supportedType())) {
            throw new IllegalArgumentException("descriptor messageType must match plugin supportedType");
        }
    }
}

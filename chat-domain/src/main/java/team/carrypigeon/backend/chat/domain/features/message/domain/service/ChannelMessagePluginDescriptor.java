package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;

/**
 * 频道消息插件治理描述。
 * 职责：表达当前内建消息插件的最小稳定治理元数据，供公开投影与后续治理边界使用。
 * 边界：这里只声明插件的静态描述、权限、事件和可用性条件，不承载运行时调度或异步执行逻辑。
 *
 * @param pluginKey 插件稳定内部标识
 * @param messageType 插件负责的消息类型
 * @param publicPluginKey 对外公开的稳定插件标识
 * @param description 插件公开描述
 * @param publicVisible 是否允许公开暴露到 public_plugins
 * @param declaredEvents 当前声明的异步事件边界列表
 * @param declaredPermissions 当前声明的权限标识列表
 * @param availabilityCondition 当前插件可用性条件说明
 */
public record ChannelMessagePluginDescriptor(
        String pluginKey,
        String messageType,
        String publicPluginKey,
        String description,
        boolean publicVisible,
        List<String> declaredEvents,
        List<String> declaredPermissions,
        String availabilityCondition
) {

    public ChannelMessagePluginDescriptor {
        requireNonBlank(pluginKey, "pluginKey");
        requireNonBlank(messageType, "messageType");
        requireNonBlank(publicPluginKey, "publicPluginKey");
        requireNonBlank(description, "description");
        requireNonBlank(availabilityCondition, "availabilityCondition");
        declaredEvents = List.copyOf(declaredEvents);
        declaredPermissions = List.copyOf(declaredPermissions);
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}

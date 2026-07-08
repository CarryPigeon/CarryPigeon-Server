package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;

/**
 * 消息类型插件注册支持。
 * 职责：收敛消息类型配置类中重复出现的 descriptor 与 registration 构造逻辑。
 * 边界：这里只提供注册对象构造支持，不承载具体消息处理规则或 Spring 装配流程。
 */
public final class MessageTypePluginRegistrationSupport {

    private MessageTypePluginRegistrationSupport() {
    }

    /**
     * 创建消息类型插件注册项。
     *
     * @param pluginKey 内部稳定插件标识
     * @param messageType 消息类型
     * @param publicPluginKey 对外公开插件标识
     * @param description 描述信息
     * @param publicVisible 是否公开暴露
     * @param declaredPermissions 声明权限
     * @param availabilityCondition 可用性说明
     * @param plugin 消息处理器
     * @return 稳定注册项
     */
    public static ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            String description,
            boolean publicVisible,
            List<String> declaredPermissions,
            String availabilityCondition,
            ChannelMessagePlugin plugin
    ) {
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        publicPluginKey,
                        description,
                        publicVisible,
                        List.of("message.sent", "message.updated", "message.recalled", "message.deleted"),
                        declaredPermissions,
                        availabilityCondition
                ),
                plugin
        );
    }
}

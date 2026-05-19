package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginGovernanceProperties;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 自定义消息类型插件配置。
 * 职责：声明 custom 消息类型的处理器与注册项。
 * 边界：只负责 custom 消息类型的 Spring 装配，不承载其它消息类型规则。
 */
@Configuration
public class CustomMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "custom";

    /**
     * 创建自定义消息插件处理器。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 自定义消息插件处理器
     */
    @Bean
    public CustomChannelMessagePlugin customChannelMessagePlugin(JsonProvider jsonProvider) {
        return new CustomChannelMessagePlugin(jsonProvider);
    }

    /**
     * 创建自定义消息注册项。
     *
     * @param governanceProperties 消息插件治理配置
     * @param customChannelMessagePlugin 自定义消息插件处理器
     * @return custom 消息注册项；若关闭则返回 null
     */
    @Bean
    public ChannelMessagePluginRegistration customChannelMessagePluginRegistration(
            MessagePluginGovernanceProperties governanceProperties,
            CustomChannelMessagePlugin customChannelMessagePlugin
    ) {
        if (!governanceProperties.customEnabled()) {
            return null;
        }
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-custom-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in custom channel message plugin",
                true,
                java.util.List.of("message:custom:send"),
                "always_available",
                customChannelMessagePlugin
        );
    }
}

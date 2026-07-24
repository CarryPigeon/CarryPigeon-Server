package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.config.PluginGovernanceProperties;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;

/**
 * system 消息类型插件配置。
 * 职责：声明 system 消息类型的处理器与注册项。
 * 边界：只负责 system 消息类型的 Spring 装配，不承载其它消息类型规则。
 */
@Configuration
public class SystemMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "system";

    /**
     * 创建 system 消息插件处理器。
     *
     * @return system 消息插件处理器
     */
    @Bean
    public SystemChannelMessagePlugin systemChannelMessagePlugin() {
        return new SystemChannelMessagePlugin();
    }

    /**
     * 创建 system 消息注册项。
     *
     * @param governanceProperties 消息插件治理配置
     * @param systemChannelMessagePlugin system 消息插件处理器
     * @return system 消息注册项；若关闭则返回 null
     */
    @Bean
    public ChannelMessagePluginRegistration systemChannelMessagePluginRegistration(
            PluginGovernanceProperties governanceProperties,
            SystemChannelMessagePlugin systemChannelMessagePlugin
    ) {
        if (!governanceProperties.systemEnabled()) {
            return null;
        }
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-system-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in system channel message plugin",
                false,
                java.util.List.of("message:system:send"),
                "internal_only",
                systemChannelMessagePlugin
        );
    }
}

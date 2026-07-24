package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.config.PluginGovernanceProperties;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;

/**
 * 文本消息类型插件配置。
 * 职责：声明 text 消息类型的处理器与注册项。
 * 边界：只负责 text 消息类型的 Spring 装配，不承载其它消息类型规则。
 */
@Configuration
public class TextMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "text";

    /**
     * 创建文本消息插件处理器。
     *
     * @return 文本消息插件处理器
     */
    @Bean
    public TextChannelMessagePlugin textChannelMessagePlugin() {
        return new TextChannelMessagePlugin();
    }

    /**
     * 创建文本消息注册项。
     *
     * @param governanceProperties 消息插件治理配置
     * @param textChannelMessagePlugin 文本消息插件处理器
     * @return text 消息注册项；若关闭则返回 null
     */
    @Bean
    public ChannelMessagePluginRegistration textChannelMessagePluginRegistration(
            PluginGovernanceProperties governanceProperties,
            TextChannelMessagePlugin textChannelMessagePlugin
    ) {
        if (!governanceProperties.textEnabled()) {
            return null;
        }
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-text-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in text channel message plugin",
                true,
                java.util.List.of("message:text:send"),
                "always_available",
                textChannelMessagePlugin
        );
    }
}

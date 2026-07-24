package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;

/**
 * 转发消息类型插件配置。
 * 职责：声明 Core:Forward 的校验插件和注册项。
 * 边界：只负责 Forward 插件装配，不承载转发来源读取与权限编排。
 */
@Configuration
public class ForwardMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "forward";

    @Bean
    public ForwardChannelMessagePlugin forwardChannelMessagePlugin() {
        return new ForwardChannelMessagePlugin();
    }

    @Bean
    public ChannelMessagePluginRegistration forwardChannelMessagePluginRegistration(
            ForwardChannelMessagePlugin plugin
    ) {
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-forward-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in forward channel message plugin",
                false,
                java.util.List.of("message:forward:send"),
                "forward_endpoint_only",
                plugin
        );
    }
}

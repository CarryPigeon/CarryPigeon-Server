package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;

/**
 * 引用回复消息类型插件配置。
 * 职责：声明 Core:ReplyText 的校验插件和注册项。
 * 边界：只负责 ReplyText 插件装配，不承载回复消息业务编排。
 */
@Configuration
public class ReplyTextMessageTypePluginConfiguration {

    public static final String MESSAGE_TYPE = "reply-text";

    @Bean
    public ReplyTextChannelMessagePlugin replyTextChannelMessagePlugin() {
        return new ReplyTextChannelMessagePlugin();
    }

    @Bean
    public ChannelMessagePluginRegistration replyTextChannelMessagePluginRegistration(
            ReplyTextChannelMessagePlugin plugin
    ) {
        return MessageTypePluginRegistrationSupport.registration(
                "builtin-reply-text-message",
                MESSAGE_TYPE,
                MESSAGE_TYPE,
                "Built-in reply text channel message plugin",
                false,
                java.util.List.of("message:reply-text:send"),
                "always_available",
                plugin
        );
    }
}

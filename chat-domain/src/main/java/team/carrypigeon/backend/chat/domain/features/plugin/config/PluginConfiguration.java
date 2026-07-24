package team.carrypigeon.backend.chat.domain.features.plugin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;

/**
 * 消息插件装配配置。
 * 职责：在 plugin feature 内装配消息插件注册器。
 * 边界：具体消息类型插件由各自独立配置类声明，这里不再维护集中式注册大表。
 */
@Configuration
@EnableConfigurationProperties(PluginGovernanceProperties.class)
public class PluginConfiguration {

    /**
     * 创建消息插件注册器。
     *
     * @param registrations 当前运行时可用的消息类型注册项
     * @return 消息插件注册器
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(java.util.List<ChannelMessagePluginRegistration> registrations) {
        return new ChannelMessagePluginRegistry(registrations);
    }

}

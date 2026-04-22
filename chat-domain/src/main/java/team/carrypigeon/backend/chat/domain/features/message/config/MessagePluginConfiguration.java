package team.carrypigeon.backend.chat.domain.features.message.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;

/**
 * 消息插件装配配置。
 * 职责：在 message feature 内装配当前可用的消息插件与注册器。
 * 边界：这里只负责插件 Bean 声明，不承载消息业务编排。
 */
@Configuration
public class MessagePluginConfiguration {

    /**
     * 创建文本消息插件。
     *
     * @return 文本消息插件
     */
    @Bean
    public ChannelMessagePlugin textChannelMessagePlugin() {
        return new TextChannelMessagePlugin();
    }

    /**
     * 创建消息插件注册器。
     *
     * @param plugins 当前运行时可用插件列表
     * @return 消息插件注册器
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(List<ChannelMessagePlugin> plugins) {
        return new ChannelMessagePluginRegistry(plugins);
    }
}

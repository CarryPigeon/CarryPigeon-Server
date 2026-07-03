package team.carrypigeon.backend.chat.domain.features.channel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelRealtimePublisher;

/**
 * 频道实时发布端口默认装配。
 * 职责：在未启用 realtime 实现时提供 channel feature 的空发布器。
 * 边界：只装配 channel 领域端口默认实现，不承载 Netty 实现细节。
 */
@Configuration
public class ChannelRealtimeConfiguration {

    /**
     * 创建默认空实现频道实时发布器。
     *
     * @return 未启用 realtime 时的空发布器
     */
    @Bean
    @ConditionalOnMissingBean(ChannelRealtimePublisher.class)
    public ChannelRealtimePublisher noopChannelRealtimePublisher() {
        return new ChannelRealtimePublisher() {
        };
    }
}

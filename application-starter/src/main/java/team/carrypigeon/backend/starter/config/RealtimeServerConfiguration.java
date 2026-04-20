package team.carrypigeon.backend.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时通道启动装配。
 * 职责：在 application-starter 中完成 Netty 通道属性绑定、处理链创建与生命周期托管。
 * 边界：不承载业务控制器和消息规则，只负责运行时装配。
 */
@Configuration
@EnableConfigurationProperties(RealtimeServerProperties.class)
@ConditionalOnProperty(prefix = "cp.chat.server.realtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeServerConfiguration {

    /**
     * 创建实时通道初始化器。
     *
     * @param properties 实时通道配置
     * @param jsonProvider 项目统一 JSON 门面
     * @param idGenerator 项目统一 ID 生成器
     * @param timeProvider 项目统一时间提供器
     * @return Netty 通道初始化器
     */
    @Bean
    public RealtimeChannelInitializer realtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider
    ) {
        return new RealtimeChannelInitializer(properties, jsonProvider, idGenerator, timeProvider);
    }

    /**
     * 创建 Netty 实时服务运行时。
     *
     * @param properties 实时通道配置
     * @param initializer Netty 通道初始化器
     * @return 启动层托管的实时服务运行时
     */
    @Bean
    public RealtimeServerRuntime realtimeServerRuntime(
            RealtimeServerProperties properties,
            RealtimeChannelInitializer initializer
    ) {
        return new RealtimeServerRuntime(properties, initializer);
    }
}

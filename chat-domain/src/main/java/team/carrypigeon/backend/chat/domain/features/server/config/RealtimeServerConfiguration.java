package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 实时通道启动装配。
 * 职责：在 server feature 内完成 Netty 通道属性绑定、处理链创建与生命周期托管。
 * 边界：不承载业务控制器和消息规则，只负责该 feature 的运行时装配。
 */
@Configuration
@EnableConfigurationProperties(RealtimeServerProperties.class)
public class RealtimeServerConfiguration {

    /**
     * 创建实时会话注册表。
     *
     * @return 当前运行时使用的实时会话注册表
     */
    @Bean
    public RealtimeSessionRegistry realtimeSessionRegistry() {
        return new RealtimeSessionRegistry();
    }

    /**
     * 创建基于 Netty 的实时消息发布器。
     *
     * @param realtimeSessionRegistry 实时会话注册表
     * @param jsonProvider 项目统一 JSON 门面
     * @param timeProvider 项目统一时间提供器
     * @return 基于 Netty 的实时消息发布器
     */
    @Bean
    @Primary
    public MessageRealtimePublisher messageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver
    ) {
        return new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                timeProvider,
                messageAttachmentPayloadResolver
        );
    }

    /**
     * 创建实时通道初始化器。
     *
     * @param properties 实时通道配置
     * @param jsonProvider 项目统一 JSON 门面
     * @param idGenerator 项目统一 ID 生成器
     * @param timeProvider 项目统一时间提供器
     * @param authTokenService 项目 access token 校验服务
     * @param realtimeSessionRegistry 实时会话注册表
     * @param messageApplicationServiceProvider 消息应用服务提供器
     * @param realtimeInboundMessageDispatcherProvider realtime 入站消息分发器提供器
     * @return Netty 通道初始化器
     */
    @Bean
    public RealtimeChannelInitializer realtimeChannelInitializer(
            RealtimeServerProperties properties,
            JsonProvider jsonProvider,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthTokenService authTokenService,
            RealtimeSessionRegistry realtimeSessionRegistry,
            ObjectProvider<team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService> messageApplicationServiceProvider,
            ObjectProvider<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherProvider
    ) {
        return new RealtimeChannelInitializer(
                properties,
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                realtimeSessionRegistry,
                messageApplicationServiceProvider,
                realtimeInboundMessageDispatcherProvider
        );
    }

    /**
     * 创建 Netty 实时服务运行时。
     *
     * @param properties 实时通道配置
     * @param initializer Netty 通道初始化器
     * @return feature 内托管的实时服务运行时
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RealtimeServerRuntime realtimeServerRuntime(
            RealtimeServerProperties properties,
            RealtimeChannelInitializer initializer
    ) {
        return new RealtimeServerRuntime(properties, initializer);
    }

    /**
     * 创建 realtime 运行时启动触发器。
     *
     * @param runtime realtime 运行时
     * @return 在单例初始化完成后触发 runtime.start 的启动器
     */
    @Bean
    public SmartInitializingSingleton realtimeServerRuntimeStarter(RealtimeServerRuntime runtime) {
        return runtime::start;
    }
}

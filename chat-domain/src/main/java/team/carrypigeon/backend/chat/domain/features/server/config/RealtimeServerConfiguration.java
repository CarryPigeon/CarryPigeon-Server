package team.carrypigeon.backend.chat.domain.features.server.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.server.domain.repository.NotificationPreferenceRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.NettyMessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeNotificationPreferenceFilter;
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
    public RealtimeDiscoverySettings realtimeDiscoverySettings(RealtimeServerProperties properties) {
        return new RealtimeDiscoverySettings(
                properties.enabled(),
                properties.host(),
                properties.port(),
                properties.path()
        );
    }

    /**
     * 创建 realtime 通知偏好过滤器。
     * 输入：已有通知偏好仓储端口和统一时间提供器。
     * 输出：发布器写入事件缓存前使用的接收人过滤组件。
     *
     * @param notificationPreferenceRepository 通知偏好仓储端口
     * @param timeProvider 项目统一时间提供器
     * @return realtime 通知偏好过滤器
     */
    @Bean
    public RealtimeNotificationPreferenceFilter realtimeNotificationPreferenceFilter(
            NotificationPreferenceRepository notificationPreferenceRepository,
            TimeProvider timeProvider
    ) {
        return new RealtimeNotificationPreferenceFilter(notificationPreferenceRepository, timeProvider);
    }

    @Bean
    @Primary
    public MessageRealtimePublisher messageRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator,
            MessagePayloadResolver messagePayloadResolver,
            RealtimeNotificationPreferenceFilter realtimeNotificationPreferenceFilter
    ) {
        return new NettyMessageRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                timeProvider,
                idGenerator,
                messagePayloadResolver,
                realtimeNotificationPreferenceFilter
        );
    }

    /**
     * 创建基于 Netty 的频道实时发布器。
     *
     * @param realtimeSessionRegistry 实时会话注册表
     * @param jsonProvider 项目统一 JSON 门面
     * @param timeProvider 项目统一时间提供器
     * @param idGenerator 项目统一 ID 生成器
     * @return 基于 Netty 的频道实时发布器
     */
    @Bean
    @Primary
    public ChannelRealtimePublisher channelRealtimePublisher(
            RealtimeSessionRegistry realtimeSessionRegistry,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            IdGenerator idGenerator,
            RealtimeNotificationPreferenceFilter realtimeNotificationPreferenceFilter
    ) {
        return new NettyChannelRealtimePublisher(
                realtimeSessionRegistry,
                jsonProvider,
                timeProvider,
                idGenerator,
                realtimeNotificationPreferenceFilter
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
     * @param channelMessagePublishingApiProvider 频道消息发布领域 API 提供器
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
            ServerIdentityProperties serverIdentityProperties,
            RealtimeSessionRegistry realtimeSessionRegistry,
            ObjectProvider<ChannelMessagePublishingApi> channelMessagePublishingApiProvider,
            ObjectProvider<RealtimeInboundMessageDispatcher> realtimeInboundMessageDispatcherProvider,
            @Value("${cp.local-dev.http.request-log.enabled:false}") boolean requestLogEnabled
    ) {
        return new RealtimeChannelInitializer(
                properties,
                jsonProvider,
                idGenerator,
                timeProvider,
                authTokenService,
                serverIdentityProperties,
                realtimeSessionRegistry,
                channelMessagePublishingApiProvider,
                realtimeInboundMessageDispatcherProvider,
                requestLogEnabled
        );
    }

    /**
     * 创建 Netty 实时服务运行时。
     *
     * @param properties 实时通道配置
     * @param initializer Netty 通道初始化器
     * @return feature 内托管的实时服务运行时
     */
    @Bean(destroyMethod = "stop")
    public RealtimeServerRuntime realtimeServerRuntime(
            RealtimeServerProperties properties,
            RealtimeChannelInitializer initializer
    ) {
        return new RealtimeServerRuntime(properties, initializer);
    }
}

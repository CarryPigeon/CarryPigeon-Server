package team.carrypigeon.backend.chat.domain.features.server.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AccessTokenAuthenticationApi;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.AuthCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.PingCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeChannelInitializer;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeCommandRegistry;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeResumeCoordinator;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.realtime.RealtimeCommandHandler;
import team.carrypigeon.backend.chat.domain.features.server.domain.capability.RealtimeEventSink;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeConnectionIndex;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeEventLog;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeEventSinkImpl;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeFrameCodec;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;

/**
 * 实时通道启动装配。
 * 职责：在 server feature 内完成连接索引、事件窗口、帧编解码、投递实现与命令注册表的装配。
 * 边界：不承载业务规则；命令处理器由本配置注入内建三项，并接受其它 feature 或插件追加。
 */
@Configuration
@EnableConfigurationProperties(RealtimeServerProperties.class)
public class RealtimeServerConfiguration {

    /**
     * 创建实时连接索引。
     *
     * @return 在线连接索引
     */
    @Bean
    public RealtimeConnectionIndex realtimeConnectionIndex() {
        return new RealtimeConnectionIndex();
    }

    /**
     * 创建实时事件窗口。
     *
     * @param properties 实时通道配置
     * @param timeProvider 项目统一时间提供器
     * @return 按账号隔离的事件窗口
     */
    @Bean
    public RealtimeEventLog realtimeEventLog(RealtimeServerProperties properties, TimeProviderImpl timeProvider) {
        return new RealtimeEventLog(
                timeProvider,
                properties.eventRetention().toMillis(),
                properties.maxEventsPerAccount(),
                properties.maxEventAccounts()
        );
    }

    /**
     * 创建实时帧编解码器。
     *
     * @param jsonProvider 项目统一 JSON 门面
     * @return 帧编解码器
     */
    @Bean
    public RealtimeFrameCodec realtimeFrameCodec(JsonProviderImpl jsonProvider) {
        return new RealtimeFrameCodec(jsonProvider);
    }

    /**
     * 创建实时事件投递实现。
     *
     * @param realtimeEventLog 事件窗口
     * @param realtimeConnectionIndex 连接索引
     * @param realtimeFrameCodec 帧编解码器
     * @param idGenerator 项目统一 ID 生成器
     * @param timeProvider 项目统一时间提供器
     * @param properties 实时通道配置
     * @return 事件投递实现
     */
    @Bean
    public RealtimeEventSink realtimeEventSink(
            RealtimeEventLog realtimeEventLog,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec realtimeFrameCodec,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            RealtimeServerProperties properties
    ) {
        return new RealtimeEventSinkImpl(
                realtimeEventLog,
                realtimeConnectionIndex,
                realtimeFrameCodec,
                idGenerator,
                timeProvider,
                properties.maxPayloadBytes()
        );
    }

    /**
     * 创建实时事件续传协调器。
     *
     * @param realtimeEventLog 事件窗口
     * @param realtimeFrameCodec 帧编解码器
     * @return 续传协调器
     */
    @Bean
    public RealtimeResumeCoordinator realtimeResumeCoordinator(
            RealtimeEventLog realtimeEventLog,
            RealtimeFrameCodec realtimeFrameCodec
    ) {
        return new RealtimeResumeCoordinator(realtimeEventLog, realtimeFrameCodec);
    }

    /**
     * 创建上行命令注册表。
     * 装配语义：内建 auth、reauth、ping 三项，再追加其它 feature 或插件声明的处理器；类型重复时启动失败。
     *
     * @param timeProvider 项目统一时间提供器
     * @param additionalHandlers 由其它 feature 或插件声明的命令处理器
     * @return 命令注册表
     */
    @Bean
    public RealtimeCommandRegistry realtimeCommandRegistry(
            TimeProviderImpl timeProvider,
            ObjectProvider<RealtimeCommandHandler> additionalHandlers
    ) {
        List<RealtimeCommandHandler> handlers = new ArrayList<>();
        handlers.add(new AuthCommandHandler("auth", "auth.ok", false));
        handlers.add(new AuthCommandHandler("reauth", "reauth.ok", true));
        handlers.add(new PingCommandHandler(timeProvider));
        additionalHandlers.orderedStream().forEach(handlers::add);
        return new RealtimeCommandRegistry(handlers);
    }

    /**
     * 创建实时能力发现设置。
     *
     * @param properties 实时通道配置
     * @return 实时能力发现设置
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
     * 创建实时通道初始化器。
     *
     * @param properties 实时通道配置
     * @param idGenerator 项目统一 ID 生成器
     * @param timeProvider 项目统一时间提供器
     * @param accessTokenAuthenticationApi access token 认证领域 API
     * @param serverIdentityProperties 服务端身份配置
     * @param realtimeConnectionIndex 在线连接索引
     * @param realtimeFrameCodec 帧编解码器
     * @param realtimeCommandRegistry 上行命令注册表
     * @param resumeCoordinator 续传协调器
     * @param requestLogEnabled 本地请求日志开关
     * @return Netty 通道初始化器
     */
    @Bean
    public RealtimeChannelInitializer realtimeChannelInitializer(
            RealtimeServerProperties properties,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            AccessTokenAuthenticationApi accessTokenAuthenticationApi,
            ServerIdentityProperties serverIdentityProperties,
            RealtimeConnectionIndex realtimeConnectionIndex,
            RealtimeFrameCodec realtimeFrameCodec,
            RealtimeCommandRegistry realtimeCommandRegistry,
            RealtimeResumeCoordinator resumeCoordinator,
            @Value("${cp.local-dev.http.request-log.enabled:false}") boolean requestLogEnabled
    ) {
        return new RealtimeChannelInitializer(
                properties,
                idGenerator,
                timeProvider,
                accessTokenAuthenticationApi,
                serverIdentityProperties,
                realtimeConnectionIndex,
                realtimeFrameCodec,
                realtimeCommandRegistry,
                resumeCoordinator,
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

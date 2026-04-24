package team.carrypigeon.backend.starter.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationService;
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.support.security.Argon2PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.support.security.HmacJwtAuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.support.security.Sha256TokenHasher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * Starter 回归测试装配配置。
 * 职责：在 application-starter 测试中装配消息附件链路所需的真实 Spring Bean 与测试替身。
 * 边界：只服务 starter 模块下的回归测试，不参与正式运行时装配。
 */
@TestConfiguration(proxyBeanMethods = false)
public class StarterRegressionConfiguration {

    /**
     * 创建测试用 ObjectMapper。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return objectMapper;
    }

    /**
     * 创建测试时钟。
     */
    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse("2026-04-23T00:00:00Z"), ZoneOffset.UTC);
    }

    /**
     * 创建测试时间提供器。
     */
    @Bean
    public TimeProvider timeProvider(Clock clock) {
        return new TimeProvider(clock);
    }

    /**
     * 创建测试 ID 生成器。
     */
    @Bean
    @Primary
    public IdGenerator idGenerator() {
        return new IdGenerator() {
            private long nextId = 5000L;

            @Override
            public long nextLongId() {
                nextId += 1;
                return nextId;
            }
        };
    }

    /**
     * 创建鉴权配置属性。
     */
    @Bean
    public AuthJwtProperties authJwtProperties() {
        return new AuthJwtProperties(
                "carrypigeon-local",
                "0123456789abcdef0123456789abcdef",
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
    }

    /**
     * 创建服务端身份属性。
     */
    @Bean
    public ServerIdentityProperties serverIdentityProperties() {
        return new ServerIdentityProperties("carrypigeon-local");
    }

    /**
     * 创建 HTTP 鉴权请求上下文。
     */
    @Bean
    public AuthRequestContext authRequestContext() {
        return new AuthRequestContext();
    }

    /**
     * 创建密码哈希器。
     */
    @Bean
    public PasswordHasher passwordHasher() {
        return new Argon2PasswordHasher();
    }

    /**
     * 创建令牌摘要器。
     */
    @Bean
    public TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }

    /**
     * 创建 JWT 令牌服务。
     */
    @Bean
    public AuthTokenService authTokenService(AuthJwtProperties authJwtProperties, JsonProvider jsonProvider) {
        return new HmacJwtAuthTokenService(authJwtProperties, jsonProvider);
    }

    /**
     * 创建附件 objectKey policy。
     */
    @Bean
    public MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy() {
        return new MessageAttachmentObjectKeyPolicy();
    }

    /**
     * 创建消息附件 payload resolver。
     */
    @Bean
    public MessageAttachmentPayloadResolver messageAttachmentPayloadResolver(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            JsonProvider jsonProvider
    ) {
        return new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider);
    }

    /**
     * 创建频道治理规则组件。
     */
    @Bean
    public ChannelGovernancePolicy channelGovernancePolicy() {
        return new ChannelGovernancePolicy();
    }

    /**
     * 创建测试用频道审计仓储替身。
     */
    @Bean
    public ChannelAuditLogRepository channelAuditLogRepository() {
        return channelAuditLog -> {
        };
    }

    /**
     * 创建消息实时发布器替身。
     */
    @Bean
    @Primary
    public MessageRealtimePublisher messageRealtimePublisher() {
        return (message, recipientAccountIds) -> {
        };
    }

    /**
     * 创建消息插件注册器。
     */
    @Bean
    public ChannelMessagePluginRegistry channelMessagePluginRegistry(
            ObjectStorageService objectStorageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy
    ) {
        MessagePluginConfiguration configuration = new MessagePluginConfiguration();
        List<ChannelMessagePlugin> plugins = List.of(
                configuration.textChannelMessagePlugin(),
                configuration.fileChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy),
                configuration.voiceChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy)
        );
        return configuration.channelMessagePluginRegistry(plugins);
    }

    /**
     * 创建鉴权应用服务。
     */
    @Bean
    public AuthApplicationService authApplicationService(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthJwtProperties authJwtProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new AuthApplicationService(
                authAccountRepository,
                authRefreshSessionRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                passwordHasher,
                tokenHasher,
                authTokenService,
                authJwtProperties,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    /**
     * 创建消息应用服务。
     */
    @Bean
    public MessageApplicationService messageApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        return new MessageApplicationService(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelGovernancePolicy,
                messageRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentObjectKeyPolicy,
                messageAttachmentPayloadResolver,
                serverIdentityProperties,
                idGenerator,
                timeProvider,
                transactionRunner,
                objectStorageServiceProvider
        );
    }

    /**
     * 创建鉴权控制器。
     */
    @Bean
    public AuthController authController(AuthApplicationService authApplicationService, AuthRequestContext authRequestContext) {
        return new AuthController(authApplicationService, authRequestContext);
    }

    /**
     * 创建消息控制器。
     */
    @Bean
    public ChannelMessageController channelMessageController(
            MessageApplicationService messageApplicationService,
            AuthRequestContext authRequestContext
    ) {
        return new ChannelMessageController(messageApplicationService, authRequestContext);
    }
}

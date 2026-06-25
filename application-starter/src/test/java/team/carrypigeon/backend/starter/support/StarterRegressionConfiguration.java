package team.carrypigeon.backend.starter.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelInviteRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelBanRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelAccessApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.application.service.ChannelQueryApplicationService;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelReadStateController;
import team.carrypigeon.backend.chat.domain.features.file.application.service.FileApplicationService;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageDeliveryApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageModerationApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageQueryApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationService;
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
    @Primary
    public AuthJwtProperties authJwtProperties() {
        return new AuthJwtProperties(
                "550e8400-e29b-41d4-a716-446655440000",
                "0123456789abcdef0123456789abcdef",
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        );
    }

    /**
     * 创建服务端身份属性。
     */
    @Bean
    @Primary
    public ServerIdentityProperties serverIdentityProperties() {
        return new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000");
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
     * 创建频道实时发布器替身。
     */
    @Bean
    @Primary
    public ChannelRealtimePublisher channelRealtimePublisher() {
        return new ChannelRealtimePublisher() {
        };
    }

    /**
     * 创建消息实时发布器替身。
     */
    @Bean
    @Primary
    public MessageRealtimePublisher messageRealtimePublisher() {
        return (message, senderSnapshot, recipientAccountIds) -> {
        };
    }

    /**
     * 创建提及仓储替身。
     */
    @Bean
    @ConditionalOnMissingBean(MentionRepository.class)
    public MentionRepository mentionRepository() {
        return new MentionRepository() {
            @Override
            public void save(Mention mention) {
            }

            @Override
            public java.util.List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
                return java.util.List.of();
            }

            @Override
            public boolean markAsRead(long accountId, long mentionId) {
                return false;
            }

            @Override
            public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
                return 0;
            }
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
        return new MessagePluginConfiguration().channelMessagePluginRegistry(java.util.List.of(
                registration("builtin-text-message", "text", "text", "always_available", new TextChannelMessagePlugin()),
                registration("builtin-test-extension-message", "test-extension", "test-extension", "always_available", new PluginChannelMessagePlugin("test-extension", jsonProvider)),
                registration(
                        "builtin-file-message",
                        "file",
                        "file",
                        "requires_object_storage",
                        new FileChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy)
                ),
                registration(
                        "builtin-voice-message",
                        "voice",
                        "voice",
                        "requires_object_storage",
                        new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, messageAttachmentObjectKeyPolicy)
                )
        ));
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
     * 创建用户资料应用服务。
     */
    @Bean
    public UserProfileApplicationService userProfileApplicationService(
            AuthAccountRepository authAccountRepository,
            UserProfileRepository userProfileRepository,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new UserProfileApplicationService(authAccountRepository, userProfileRepository, timeProvider, transactionRunner);
    }

    /**
     * 创建消息应用服务。
     */
    @Bean
    public MessageQueryApplicationService messageQueryApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelPinRepository channelPinRepository,
            MessageRepository messageRepository,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver
    ) {
        return new MessageQueryApplicationService(
                channelRepository,
                channelMemberRepository,
                channelPinRepository,
                messageRepository,
                messageAttachmentPayloadResolver
        );
    }

    @Bean
    public MessageDeliveryApplicationService messageDeliveryApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        return new MessageDeliveryApplicationService(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentObjectKeyPolicy,
                messageAttachmentPayloadResolver,
                serverIdentityProperties,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner,
                objectStorageServiceProvider
        );
    }

    @Bean
    public MessageModerationApplicationService messageModerationApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        return new MessageModerationApplicationService(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentObjectKeyPolicy,
                messageAttachmentPayloadResolver,
                serverIdentityProperties,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner,
                objectStorageServiceProvider
        );
    }

    @Bean
    public ChannelQueryApplicationService channelQueryApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        return new ChannelQueryApplicationService(
                channelRepository,
                channelMemberRepository,
                channelBanRepository,
                channelAuditLogRepository,
                channelReadStateRepository,
                userProfileRepository,
                channelGovernancePolicy
        );
    }

    @Bean
    public ChannelAccessApplicationService channelAccessApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            MessageRepository messageRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            ChannelRealtimePublisher channelRealtimePublisher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelAccessApplicationService(
                channelRepository,
                channelMemberRepository,
                channelInviteRepository,
                channelBanRepository,
                channelAuditLogRepository,
                channelReadStateRepository,
                messageRepository,
                userProfileRepository,
                channelGovernancePolicy,
                channelRealtimePublisher,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    /**
     * 创建鉴权控制器。
     */
    @Bean
    public AuthController authController(
            AuthApplicationService authApplicationService,
            ServerApplicationService serverApplicationService,
            AuthJwtProperties authJwtProperties
    ) {
        return new AuthController(authApplicationService, serverApplicationService, authJwtProperties);
    }

    /**
     * 创建服务发现应用服务。
     */
    @Bean
    public ServerApplicationService serverApplicationService(
            ServerIdentityProperties serverIdentityProperties,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            TimeProvider timeProvider
    ) {
        return new ServerApplicationService(
                serverIdentityProperties,
                "CarryPigeonBackend",
                channelMessagePluginRegistry,
                new RealtimeServerProperties(true, "127.0.0.1", 28080, "/api/ws", 1, 0),
                timeProvider,
                java.util.List.of()
        );
    }

    /**
     * 创建消息控制器。
     */
    @Bean
    public ChannelMessageController channelMessageController(
            MessageDeliveryApplicationService messageDeliveryApplicationService,
            MessageModerationApplicationService messageModerationApplicationService,
            MessageQueryApplicationService messageQueryApplicationService,
            UserProfileApplicationService userProfileApplicationService,
            AuthRequestContext authRequestContext,
            JsonProvider jsonProvider
    ) {
        return new ChannelMessageController(
                messageDeliveryApplicationService,
                messageModerationApplicationService,
                messageQueryApplicationService,
                userProfileApplicationService,
                authRequestContext,
                jsonProvider
        );
    }

    @Bean
    public FileApplicationService fileApplicationService(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            ChannelMemberRepository channelMemberRepository,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            AuthJwtProperties authJwtProperties
    ) {
        return new FileApplicationService(
                objectStorageServiceProvider,
                channelMemberRepository,
                idGenerator,
                timeProvider,
                authJwtProperties
        );
    }

    @Bean
    public FileController fileController(FileApplicationService fileApplicationService, AuthRequestContext authRequestContext) {
        return new FileController(fileApplicationService, authRequestContext);
    }

    @Bean
    public ChannelReadStateController channelReadStateController(
            ChannelAccessApplicationService channelAccessApplicationService,
            ChannelQueryApplicationService channelQueryApplicationService,
            AuthRequestContext authRequestContext
    ) {
        return new ChannelReadStateController(channelAccessApplicationService, channelQueryApplicationService, authRequestContext);
    }

    private static <T> ObjectProvider<T> objectProvider(T object) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return object;
            }

            @Override
            public T getIfAvailable() {
                return object;
            }

            @Override
            public T getIfUnique() {
                return object;
            }

            @Override
            public T getObject() {
                return object;
            }
        };
    }

    private static ChannelMessagePluginRegistration registration(
            String pluginKey,
            String messageType,
            String publicPluginKey,
            String availabilityCondition,
            team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin plugin
    ) {
        return new ChannelMessagePluginRegistration(
                new ChannelMessagePluginDescriptor(
                        pluginKey,
                        messageType,
                        publicPluginKey,
                        "test plugin",
                        true,
                        java.util.List.of("message.sent", "message.recalled"),
                        java.util.List.of("message:" + messageType + ":send"),
                        availabilityCondition
                ),
                plugin
        );
    }
}

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
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthJwtProperties;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.EmailVerificationCodeService;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthAccountDomainApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthSessionDomainApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthTokenSettings;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelMessageBoundary;
import team.carrypigeon.backend.chat.domain.features.channel.domain.port.ChannelRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelAccessDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelQueryDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.support.message.MessageBackedChannelMessageBoundary;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelReadStateController;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.port.FileAttachmentAccessAuthorizer;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileTransferDomainApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileUploadShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageLifecycleDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageAttachmentDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageTimelineDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePublishingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.message.support.channel.ChannelBackedMessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.ServerEntranceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeServerProperties;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserProfileDomainApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;
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
     * 创建领域侧 token 签发设置。
     */
    @Bean
    public AuthTokenSettings authTokenSettings(AuthJwtProperties authJwtProperties) {
        return new AuthTokenSettings(authJwtProperties.accessTokenTtl(), authJwtProperties.refreshTokenTtl());
    }

    /**
     * 创建测试验证码服务替身。
     */
    @Bean
    public EmailVerificationCodeService emailVerificationCodeService() {
        return new EmailVerificationCodeService() {
            @Override
            public void issueCode(String email) {
            }

            @Override
            public void verifyCode(String email, String code) {
            }
        };
    }

    /**
     * 创建鉴权账号领域 API。
     */
    @Bean
    public AuthAccountApi authAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationCodeService emailVerificationCodeService
    ) {
        return new AuthAccountDomainApi(
                authAccountRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                passwordHasher,
                idGenerator,
                timeProvider,
                transactionRunner,
                emailVerificationCodeService
        );
    }

    /**
     * 创建鉴权会话领域 API。
     */
    @Bean
    public AuthSessionApi authSessionDomainApi(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserProfileRepository userProfileRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenService authTokenService,
            AuthTokenSettings authTokenSettings,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationCodeService emailVerificationCodeService
    ) {
        return new AuthSessionDomainApi(
                authAccountRepository,
                authRefreshSessionRepository,
                userProfileRepository,
                channelRepository,
                channelMemberRepository,
                passwordHasher,
                tokenHasher,
                authTokenService,
                authTokenSettings,
                idGenerator,
                timeProvider,
                transactionRunner,
                emailVerificationCodeService
        );
    }

    /**
     * 创建用户资料领域 API。
     */
    @Bean
    public UserProfileApi userProfileDomainApi(
            AuthAccountRepository authAccountRepository,
            UserProfileRepository userProfileRepository,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new UserProfileDomainApi(authAccountRepository, userProfileRepository, timeProvider, transactionRunner);
    }

    /**
     * 创建消息侧频道边界适配器。
     */
    @Bean
    public MessageChannelBoundary messageChannelBoundary(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        return new ChannelBackedMessageChannelBoundary(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy
        );
    }

    @Bean
    public ChannelMessageBoundary channelMessageBoundary(MessageRepository messageRepository) {
        return new MessageBackedChannelMessageBoundary(messageRepository);
    }

    @Bean
    public ServerIdentityProvider serverIdentityProvider(ServerIdentityProperties serverIdentityProperties) {
        return serverIdentityProperties;
    }

    @Bean
    public ChannelMessagePublishingApi channelMessagePublishingDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessagePublishingDomainApi(
                messageChannelBoundary,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public ChannelMessageLifecycleApi channelMessageLifecycleDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessageLifecycleDomainApi(
                messageChannelBoundary,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public ChannelMessageTimelineApi channelMessageTimelineDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessageTimelineDomainApi(
                messageChannelBoundary,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public ChannelMessageAttachmentDomainApi channelMessageAttachmentDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        return new ChannelMessageAttachmentDomainApi(
                messageChannelBoundary,
                messageAttachmentObjectKeyPolicy,
                idGenerator,
                timeProvider,
                objectStorageServiceProvider
        );
    }

    @Bean
    public ChannelQueryDomainApi channelQueryDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        return new ChannelQueryDomainApi(
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
    public ChannelAccessDomainApi channelAccessDomainApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelInviteRepository channelInviteRepository,
            ChannelBanRepository channelBanRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelReadStateRepository channelReadStateRepository,
            ChannelMessageBoundary channelMessageBoundary,
            UserProfileRepository userProfileRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            ChannelRealtimePublisher channelRealtimePublisher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelAccessDomainApi(
                channelRepository,
                channelMemberRepository,
                channelInviteRepository,
                channelBanRepository,
                channelAuditLogRepository,
                channelReadStateRepository,
                channelMessageBoundary,
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
            AuthAccountApi authAccountDomainApi,
            AuthSessionApi authSessionDomainApi,
            ServerEntranceApi serverEntranceDomainApi,
            AuthTokenSettings authTokenSettings
    ) {
        return new AuthController(authAccountDomainApi, authSessionDomainApi, serverEntranceDomainApi, authTokenSettings);
    }

    /**
     * 创建服务入口领域 API。
     */
    @Bean
    public ServerEntranceApi serverEntranceDomainApi(
            ServerIdentityProvider serverIdentityProvider,
            TimeProvider timeProvider
    ) {
        return new ServerEntranceDomainApi(
                serverIdentityProvider,
                "CarryPigeonBackend",
                new team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings(true, "127.0.0.1", 28080, "/api/ws"),
                timeProvider,
                java.util.List.of()
        );
    }

    /**
     * 创建消息控制器。
     */
    @Bean
    public ChannelMessageController channelMessageController(
            ChannelMessagePublishingApi channelMessagePublishingDomainApi,
            ChannelMessageTimelineApi channelMessageTimelineDomainApi,
            ChannelMessageAttachmentDomainApi channelMessageAttachmentDomainApi,
            team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi channelMessageLifecycleDomainApi,
            AuthRequestContext authRequestContext,
            ChannelMessageV1ResponseMapper responseMapper
    ) {
        return new ChannelMessageController(
                channelMessagePublishingDomainApi,
                channelMessageTimelineDomainApi,
                channelMessageAttachmentDomainApi,
                channelMessageLifecycleDomainApi,
                authRequestContext,
                responseMapper
        );
    }

    @Bean
    public ChannelMessageV1ResponseMapper channelMessageV1ResponseMapper(
            UserProfileApi userProfileDomainApi,
            JsonProvider jsonProvider
    ) {
        return new ChannelMessageV1ResponseMapper(userProfileDomainApi, jsonProvider);
    }

    @Bean
    public FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer(ChannelMemberRepository channelMemberRepository) {
        return new team.carrypigeon.backend.chat.domain.features.file.support.ChannelMemberFileAttachmentAccessAuthorizer(channelMemberRepository);
    }

    @Bean
    public FileUploadShareKeyCodec fileUploadShareKeyCodec(AuthJwtProperties authJwtProperties) {
        return new FileUploadShareKeyCodec(authJwtProperties.secret());
    }

    @Bean
    public FileTransferApi fileTransferDomainApi(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            FileAttachmentAccessAuthorizer fileAttachmentAccessAuthorizer,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            FileUploadShareKeyCodec fileUploadShareKeyCodec
    ) {
        return new FileTransferDomainApi(
                objectStorageServiceProvider,
                fileAttachmentAccessAuthorizer,
                idGenerator,
                timeProvider,
                fileUploadShareKeyCodec
        );
    }

    @Bean
    public FileController fileController(FileTransferApi fileTransferDomainApi, AuthRequestContext authRequestContext) {
        return new FileController(fileTransferDomainApi, authRequestContext);
    }

    @Bean
    public ChannelReadStateController channelReadStateController(
            ChannelAccessDomainApi channelAccessDomainApi,
            ChannelQueryDomainApi channelQueryDomainApi,
            AuthRequestContext authRequestContext
    ) {
        return new ChannelReadStateController(channelAccessDomainApi, channelQueryDomainApi, authRequestContext);
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
            ChannelMessagePlugin plugin
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

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
import team.carrypigeon.backend.chat.domain.features.auth.config.AuthPasswordLoginProperties;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.shared.controller.support.RequestAuthenticationContext;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.AuthTokenCodec;
import team.carrypigeon.backend.chat.domain.features.verification.domain.api.EmailVerificationApi;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.IssueEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.features.verification.domain.command.VerifyEmailVerificationCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.PasswordHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.capability.TokenHasher;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthAccountRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.repository.AuthRefreshSessionRepository;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthAccountDomainApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.service.AuthPasswordLoginPolicy;
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
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelAccountProvisioningDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelMessagingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReferenceApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.MessageReferenceResult;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelAccessDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelQueryDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageReferenceDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelReadStateController;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileTransferApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileReferenceDomainApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileTransferDomainApi;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileUploadShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.plugin.config.PluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageTimelineApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageLifecycleDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageAttachmentDomainApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.MessageDomainPluginApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginCatalogApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.MessageDomainPluginDomainApi;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.PluginCatalogDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessageTimelineDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePublishingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.controller.support.ChannelMessageV1ResponseMapper;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.ForwardChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.ReplyTextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.service.ServerEntranceDomainApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserProfileApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.api.UserAccountProvisioningApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserProfileDomainApi;
import team.carrypigeon.backend.chat.domain.features.user.domain.service.UserAccountProvisioningDomainApi;
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
     * 创建用户名密码登录开关配置。
     */
    @Bean
    @Primary
    public AuthPasswordLoginProperties authPasswordLoginProperties() {
        return new AuthPasswordLoginProperties(true);
    }

    /**
     * 创建用户名密码登录策略。
     */
    @Bean
    @Primary
    public AuthPasswordLoginPolicy authPasswordLoginPolicy(AuthPasswordLoginProperties properties) {
        return new AuthPasswordLoginPolicy(properties.isEnabled());
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
    public RequestAuthenticationContext authRequestContext() {
        return new RequestAuthenticationContext();
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
    public AuthTokenCodec authTokenService(AuthJwtProperties authJwtProperties, JsonProvider jsonProvider) {
        return new HmacJwtAuthTokenService(authJwtProperties, jsonProvider);
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

    @Bean
    public RealtimeEventApi realtimeEventApi() {
        return command -> {
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
            public void deleteByMessageId(long messageId) {
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
            FileReferenceApi fileReferenceApi
    ) {
        return new PluginConfiguration().channelMessagePluginRegistry(java.util.List.of(
                registration("builtin-text-message", "text", "text", "always_available", new TextChannelMessagePlugin()),
                registration("builtin-reply-text-message", "reply-text", "reply-text", "always_available", new ReplyTextChannelMessagePlugin()),
                registration("builtin-forward-message", "forward", "forward", "forward_endpoint_only", new ForwardChannelMessagePlugin()),
                registration("builtin-test-extension-message", "test-extension", "test-extension", "always_available", new PluginChannelMessagePlugin("test-extension")),
                registration(
                        "builtin-file-message",
                        "file",
                        "file",
                        "requires_object_storage",
                        new FileChannelMessagePlugin(objectStorageService, fileReferenceApi)
                ),
                registration(
                        "builtin-voice-message",
                        "voice",
                        "voice",
                        "requires_object_storage",
                        new VoiceChannelMessagePlugin(objectStorageService, fileReferenceApi)
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
    public EmailVerificationApi emailVerificationApi() {
        return new EmailVerificationApi() {
            @Override
            public void issueCode(IssueEmailVerificationCodeCommand command) {
            }

            @Override
            public void verifyCode(VerifyEmailVerificationCodeCommand command) {
            }
        };
    }

    @Bean
    public UserAccountProvisioningApi userAccountProvisioningApi(UserProfileRepository userProfileRepository) {
        return new UserAccountProvisioningDomainApi(userProfileRepository);
    }

    @Bean
    public ChannelAccountProvisioningApi channelAccountProvisioningApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository
    ) {
        return new ChannelAccountProvisioningDomainApi(channelRepository, channelMemberRepository);
    }

    /**
     * 创建鉴权账号领域 API。
     */
    @Bean
    public AuthAccountApi authAccountDomainApi(
            AuthAccountRepository authAccountRepository,
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelAccountProvisioningApi channelAccountProvisioningApi,
            PasswordHasher passwordHasher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new AuthAccountDomainApi(
                authAccountRepository,
                userAccountProvisioningApi,
                channelAccountProvisioningApi,
                passwordHasher,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    /**
     * 创建鉴权会话领域 API。
     */
    @Bean
    public AuthSessionApi authSessionDomainApi(
            AuthAccountRepository authAccountRepository,
            AuthRefreshSessionRepository authRefreshSessionRepository,
            UserAccountProvisioningApi userAccountProvisioningApi,
            ChannelAccountProvisioningApi channelAccountProvisioningApi,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            AuthTokenCodec authTokenService,
            AuthTokenSettings authTokenSettings,
            AuthPasswordLoginPolicy authPasswordLoginPolicy,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            EmailVerificationApi emailVerificationApi
    ) {
        return new AuthSessionDomainApi(
                authAccountRepository,
                authRefreshSessionRepository,
                userAccountProvisioningApi,
                channelAccountProvisioningApi,
                passwordHasher,
                tokenHasher,
                authTokenService,
                authTokenSettings,
                authPasswordLoginPolicy,
                idGenerator,
                timeProvider,
                transactionRunner,
                emailVerificationApi
        );
    }

    /**
     * 创建用户资料领域 API。
     */
    @Bean
    public UserProfileApi userProfileDomainApi(
            AuthAccountApi authAccountApi,
            UserProfileRepository userProfileRepository,
            EmailVerificationApi emailVerificationApi,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new UserProfileDomainApi(
                authAccountApi,
                userProfileRepository,
                emailVerificationApi,
                timeProvider,
                transactionRunner
        );
    }

    /**
     * 创建消息侧频道边界适配器。
     */
    @Bean
    public ChannelMessagingApi channelMessagingApi(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelPinRepository channelPinRepository,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        return new ChannelMessagingDomainApi(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy
        );
    }

    @Bean
    public MessageReferenceApi messageReferenceApi(MessageRepository messageRepository) {
        return new MessageReferenceDomainApi(messageRepository);
    }

    @Bean
    public FileReferenceApi fileReferenceApi() {
        return new FileReferenceDomainApi();
    }

    @Bean
    public ServerIdentityProvider serverIdentityProvider(ServerIdentityProperties serverIdentityProperties) {
        return serverIdentityProperties;
    }

    @Bean
    public ChannelMessagePublishingApi channelMessagePublishingDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageIdempotencyRepository messageIdempotencyRepository,
            RealtimeEventApi realtimeEventApi,
            MessageDomainPluginApi messageDomainPluginApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessagePublishingDomainApi(
                channelMessagingApi,
                messageRepository,
                mentionRepository,
                messageIdempotencyRepository,
                realtimeEventApi,
                messageDomainPluginApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public MessageIdempotencyRepository messageIdempotencyRepository() {
        return new StarterMessageIdempotencyRepository();
    }

    @Bean
    public ChannelMessageLifecycleApi channelMessageLifecycleDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessageLifecycleDomainApi(
                channelMessagingApi,
                messageRepository,
                mentionRepository,
                realtimeEventApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public ChannelMessageTimelineApi channelMessageTimelineDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        return new ChannelMessageTimelineDomainApi(
                channelMessagingApi,
                messageRepository,
                mentionRepository,
                realtimeEventApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    @Bean
    public ChannelMessageAttachmentDomainApi channelMessageAttachmentDomainApi(
            ChannelMessagingApi channelMessagingApi,
            FileReferenceApi fileReferenceApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        return new ChannelMessageAttachmentDomainApi(
                channelMessagingApi,
                fileReferenceApi,
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
            UserProfileApi userProfileApi,
            ChannelGovernancePolicy channelGovernancePolicy
    ) {
        return new ChannelQueryDomainApi(
                channelRepository,
                channelMemberRepository,
                channelBanRepository,
                channelAuditLogRepository,
                channelReadStateRepository,
                userProfileApi,
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
            MessageReferenceApi messageReferenceApi,
            UserProfileApi userProfileApi,
            ChannelGovernancePolicy channelGovernancePolicy,
            RealtimeEventApi realtimeEventApi,
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
                messageReferenceApi,
                userProfileApi,
                channelGovernancePolicy,
                realtimeEventApi,
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
            PluginCatalogApi pluginCatalogApi
    ) {
        return new AuthController(
                authAccountDomainApi,
                authSessionDomainApi,
                pluginCatalogApi
        );
    }

    /**
     * 创建服务入口领域 API。
     */
    @Bean
    public ServerEntranceApi serverEntranceDomainApi(
            ServerIdentityProvider serverIdentityProvider,
            TimeProvider timeProvider,
            PluginCatalogApi pluginCatalogApi
    ) {
        return new ServerEntranceDomainApi(
                serverIdentityProvider,
                "CarryPigeonBackend",
                new team.carrypigeon.backend.chat.domain.features.server.domain.service.RealtimeDiscoverySettings(true, "127.0.0.1", 28080, "/api/ws"),
                timeProvider,
                pluginCatalogApi
        );
    }

    @Bean
    public MessageDomainPluginApi messageDomainPluginApi(ChannelMessagePluginRegistry pluginRegistry) {
        return new MessageDomainPluginDomainApi(pluginRegistry);
    }

    @Bean
    public PluginCatalogApi pluginCatalogApi(ChannelMessagePluginRegistry pluginRegistry) {
        return new PluginCatalogDomainApi(pluginRegistry);
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
            RequestAuthenticationContext authRequestContext,
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
    public ChannelMessageV1ResponseMapper channelMessageV1ResponseMapper() {
        return new ChannelMessageV1ResponseMapper();
    }

    @Bean
    public FileUploadShareKeyCodec fileUploadShareKeyCodec() {
        return new FileUploadShareKeyCodec("0123456789abcdef0123456789abcdef");
    }

    @Bean
    public FileTransferApi fileTransferDomainApi(
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider,
            ChannelMessagingApi channelMessagingApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            FileUploadShareKeyCodec fileUploadShareKeyCodec
    ) {
        return new FileTransferDomainApi(
                objectStorageServiceProvider,
                channelMessagingApi,
                idGenerator,
                timeProvider,
                fileUploadShareKeyCodec
        );
    }

    @Bean
    public FileController fileController(FileTransferApi fileTransferDomainApi, RequestAuthenticationContext authRequestContext) {
        return new FileController(fileTransferDomainApi, authRequestContext);
    }

    @Bean
    public ChannelReadStateController channelReadStateController(
            ChannelAccessDomainApi channelAccessDomainApi,
            ChannelQueryDomainApi channelQueryDomainApi,
            RequestAuthenticationContext authRequestContext
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

    /**
     * Starter 回归环境的消息幂等仓储替身。
     * 职责：为不发送幂等转发请求的启动测试提供完整装配依赖。
     */
    private static final class StarterMessageIdempotencyRepository implements MessageIdempotencyRepository {

        @Override
        public MessageIdempotency reserve(MessageIdempotency reservation) {
            return reservation;
        }

        @Override
        public void complete(
                long accountId,
                String operation,
                String idempotencyKey,
                String requestFingerprint,
                long messageId,
                Instant completedAt
        ) {
        }
    }
}

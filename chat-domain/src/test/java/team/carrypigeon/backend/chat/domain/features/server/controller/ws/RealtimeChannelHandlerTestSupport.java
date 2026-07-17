package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePublishingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.channel.ChannelBackedMessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.CustomChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.ChannelMessageRealtimeInboundHandler;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthAccount;
import team.carrypigeon.backend.chat.domain.features.auth.domain.model.AuthTokenClaims;
import team.carrypigeon.backend.chat.domain.features.auth.domain.port.AuthTokenService;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * RealtimeChannelHandler 测试支持。
 * 职责：为 realtime handler 的生命周期与消息分发契约测试提供共享通道与替身构建逻辑。
 * 边界：仅服务当前 WebSocket handler 测试，不扩展为跨 feature 的通用测试基建。
 */
final class RealtimeChannelHandlerTestSupport {

    private RealtimeChannelHandlerTestSupport() {
    }

    static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }

    static RealtimeInboundMessageDispatcher dispatcher(JsonProvider jsonProvider) {
        return new RealtimeInboundMessageDispatcher(List.of(new ChannelMessageRealtimeInboundHandler(jsonProvider)));
    }

    static EmbeddedChannel channel(RealtimeSessionRegistry registry, ChannelMessagePublishingApi service) {
        Supplier<ChannelMessagePublishingApi> supplier = () -> service;
        JsonProvider jsonProvider = jsonProvider();
        RealtimeInboundMessageDispatcher dispatcher = dispatcher(jsonProvider);
        return new EmbeddedChannel(new RealtimeChannelHandler(
                jsonProvider,
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                authTokenService(),
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                registry,
                supplier,
                () -> dispatcher
        ));
    }

    private static MessageChannelBoundary messageChannelBoundary(List<Long> recipientAccountIds) {
        return new ChannelBackedMessageChannelBoundary(
                new ChannelRepository() {
                    @Override
                    public Optional<Channel> findDefaultChannel() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Channel> findSystemChannel() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Channel> findById(long channelId) {
                        return Optional.of(new Channel(
                                1L, 1L, "public", "", "", "", "public", true,
                                Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
                        ));
                    }
                },
                new ChannelMemberRepository() {
                    @Override
                    public boolean exists(long channelId, long accountId) {
                        return true;
                    }

                    @Override
                    public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
                        return Optional.of(new ChannelMember(
                                channelId,
                                accountId,
                                ChannelMemberRole.MEMBER,
                                Instant.parse("2026-04-22T00:00:00Z"),
                                null
                        ));
                    }

                    @Override
                    public void save(ChannelMember channelMember) {
                    }

                    @Override
                    public List<Long> findAccountIdsByChannelId(long channelId) {
                        return recipientAccountIds;
                    }
                },
                new ChannelAuditLogRepository() {
                    @Override
                    public void append(ChannelAuditLog channelAuditLog) {
                    }
                },
                new ChannelPinRepository() {
                    @Override
                    public java.util.Optional<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin channelPin) {
                    }

                    @Override
                    public void delete(long channelId, long messageId) {
                    }

                    @Override
                    public java.util.List<team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return java.util.List.of();
                    }

                    @Override
                    public long countByChannelId(long channelId) {
                        return 0;
                    }
                },
                new ChannelGovernancePolicy()
        );
    }

    static ChannelMessagePublishingApi service(RealtimeSessionRegistry registry) {
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        ChannelMessagePluginRegistry pluginRegistry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text-message", "text", "text", "always_available", new TextChannelMessagePlugin()),
                registration("builtin-test-extension-message", "test-extension", "test-extension", "always_available", new PluginChannelMessagePlugin("test-extension", jsonProvider)),
                registration(
                        "builtin-file-message",
                        "file",
                        "file",
                        "requires_object_storage",
                        new FileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                ),
                registration(
                        "builtin-voice-message",
                        "voice",
                        "voice",
                        "requires_object_storage",
                        new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                )
        ));
        return channelMessageApi(
                messageChannelBoundary(List.of(1001L, 1002L)),
                new MessageRepository() {
                    @Override
                    public ChannelMessage save(ChannelMessage message) {
                        return message;
                    }

                    @Override
                    public Optional<ChannelMessage> findById(long messageId) {
                        return Optional.empty();
                    }

                    @Override
                    public ChannelMessage update(ChannelMessage message) {
                        return message;
                    }

                    @Override
                    public void delete(long messageId) {
                    }

                    @Override
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                mentionRepository(),
                userProfileRepository(),
                new MessageRealtimePublisher() {
                    @Override
                    public void publish(
                            ChannelMessage message,
                            MessageSenderSnapshot senderSnapshot,
                            java.util.Collection<Long> recipientAccountIds
                    ) {
                        String payload = jsonProvider().toJson(new RealtimeServerMessage(
                                "event",
                                null,
                                Map.of(
                                        "event_id", "9001",
                                        "event_type", "message.created",
                                        "server_time", Instant.parse("2026-04-22T00:00:00Z").toEpochMilli(),
                                        "payload", Map.of("message", message)
                                ),
                                null
                        ));
                        for (Long recipientAccountId : recipientAccountIds) {
                            registry.getChannels(recipientAccountId).forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(payload)));
                        }
                    }

                    @Override
                    public void publishUpdate(
                            ChannelMessage message,
                            MessageSenderSnapshot senderSnapshot,
                            java.util.Collection<Long> recipientAccountIds
                    ) {
                        publish(message, senderSnapshot, recipientAccountIds);
                    }
                },
                pluginRegistry,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new TransactionRunner() {
                    @Override
                    public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public void runInTransaction(Runnable action) {
                        action.run();
                    }
                }
        );
    }

    static ChannelMessagePublishingApi failingService() {
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        return channelMessageApi(
                messageChannelBoundary(List.of(1001L)),
                new MessageRepository() {
                    @Override
                    public ChannelMessage save(ChannelMessage message) {
                        throw new IllegalStateException("boom");
                    }

                    @Override
                    public Optional<ChannelMessage> findById(long messageId) {
                        return Optional.empty();
                    }

                    @Override
                    public ChannelMessage update(ChannelMessage message) {
                        throw new IllegalStateException("boom");
                    }

                    @Override
                    public void delete(long messageId) {
                        throw new IllegalStateException("boom");
                    }

                    @Override
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                mentionRepository(),
                userProfileRepository(),
                (message, senderSnapshot, recipientAccountIds) -> {
                },
                new ChannelMessagePluginRegistry(List.of(
                        registration("builtin-text-message", "text", "text", "always_available", new TextChannelMessagePlugin()),
                        registration("builtin-test-extension-message", "test-extension", "test-extension", "always_available", new PluginChannelMessagePlugin("test-extension", jsonProvider)),
                        registration("builtin-custom-message", "custom", "custom", "always_available", new CustomChannelMessagePlugin(jsonProvider)),
                        registration(
                                "builtin-file-message",
                                "file",
                                "file",
                                "requires_object_storage",
                                new FileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                        ),
                        registration(
                                "builtin-voice-message",
                                "voice",
                                "voice",
                                "requires_object_storage",
                                new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                        )
                )),
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new TransactionRunner() {
                    @Override
                    public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public void runInTransaction(Runnable action) {
                        action.run();
                    }
                }
        );
    }

    static ChannelMessagePublishingApi serviceWithoutExtensionRegistration(RealtimeSessionRegistry registry) {
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        ChannelMessagePluginRegistry pluginRegistry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text-message", "text", "text", "always_available", new TextChannelMessagePlugin()),
                registration(
                        "builtin-file-message",
                        "file",
                        "file",
                        "requires_object_storage",
                        new FileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                ),
                registration(
                        "builtin-voice-message",
                        "voice",
                        "voice",
                        "requires_object_storage",
                        new VoiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                )
        ));
        return channelMessageApi(
                messageChannelBoundary(List.of(1001L, 1002L)),
                new MessageRepository() {
                    @Override
                    public ChannelMessage save(ChannelMessage message) {
                        return message;
                    }

                    @Override
                    public Optional<ChannelMessage> findById(long messageId) {
                        return Optional.empty();
                    }

                    @Override
                    public ChannelMessage update(ChannelMessage message) {
                        return message;
                    }

                    @Override
                    public void delete(long messageId) {
                    }

                    @Override
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                mentionRepository(),
                userProfileRepository(),
                new MessageRealtimePublisher() {
                    @Override
                    public void publish(
                            ChannelMessage message,
                            MessageSenderSnapshot senderSnapshot,
                            java.util.Collection<Long> recipientAccountIds
                    ) {
                        String payload = jsonProvider().toJson(new RealtimeServerMessage(
                                "event",
                                null,
                                Map.of(
                                        "event_id", "9001",
                                        "event_type", "message.created",
                                        "server_time", Instant.parse("2026-04-22T00:00:00Z").toEpochMilli(),
                                        "payload", Map.of("message", message)
                                ),
                                null
                        ));
                        for (Long recipientAccountId : recipientAccountIds) {
                            registry.getChannels(recipientAccountId).forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(payload)));
                        }
                    }

                    @Override
                    public void publishUpdate(
                            ChannelMessage message,
                            MessageSenderSnapshot senderSnapshot,
                            java.util.Collection<Long> recipientAccountIds
                    ) {
                        publish(message, senderSnapshot, recipientAccountIds);
                    }
                },
                pluginRegistry,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                jsonProvider,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new TransactionRunner() {
                    @Override
                    public <T> T runInTransaction(java.util.function.Supplier<T> action) {
                        return action.get();
                    }

                    @Override
                    public void runInTransaction(Runnable action) {
                        action.run();
                    }
                }
        );
    }

    private static ChannelMessagePublishingApi channelMessageApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry pluginRegistry,
            MessageAttachmentPayloadResolver payloadResolver,
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
                pluginRegistry,
                payloadResolver,
                new ServerIdentityProperties("550e8400-e29b-41d4-a716-446655440000"),
                () -> 7001L,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
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
                        List.of("message.sent", "message.recalled"),
                        List.of("message:" + messageType + ":send"),
                        availabilityCondition
                ),
                plugin
        );
    }

    private static MentionRepository mentionRepository() {
        return new MentionRepository() {
            @Override
            public void save(Mention mention) {
            }

            @Override
            public void deleteByMessageId(long messageId) {
            }

            @Override
            public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
                return List.of();
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

    private static AuthTokenService authTokenService() {
        return new AuthTokenService() {
            @Override
            public String issueAccessToken(AuthAccount account, Instant expiresAt) {
                return "access-token";
            }

            @Override
            public String issueRefreshToken(AuthAccount account, long refreshSessionId, Instant expiresAt) {
                return "refresh-token";
            }

            @Override
            public AuthTokenClaims parseAccessToken(String accessToken) {
                if ("bad-subject-token".equals(accessToken)) {
                    return new AuthTokenClaims("not-a-number", "carry-user@example.com", "access", 0L, Instant.parse("2026-04-22T00:30:00Z"));
                }
                if ("access-token-2".equals(accessToken)) {
                    return new AuthTokenClaims("1002", "carry-ops@example.com", "access", 0L, Instant.parse("2026-04-22T00:30:00Z"));
                }
                return new AuthTokenClaims("1001", "carry-user@example.com", "access", 0L, Instant.parse("2026-04-22T00:30:00Z"));
            }

            @Override
            public AuthTokenClaims parseRefreshToken(String refreshToken) {
                return new AuthTokenClaims("1001", "carry-user@example.com", "refresh", 2001L, Instant.parse("2026-05-04T12:00:00Z"));
            }
        };
    }

    private static ObjectStorageService storageService() {
        return new ObjectStorageService() {
            @Override
            public StorageObject put(PutObjectCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<StorageObject> get(GetObjectCommand command) {
                return Optional.of(StorageObject.metadata(command.objectKey(), "application/octet-stream", 1024));
            }

            @Override
            public void delete(team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static ObjectProvider<ObjectStorageService> objectProvider(ObjectStorageService objectStorageService) {
        return new ObjectProvider<>() {
            @Override
            public ObjectStorageService getObject(Object... args) {
                return objectStorageService;
            }

            @Override
            public ObjectStorageService getIfAvailable() {
                return objectStorageService;
            }

            @Override
            public ObjectStorageService getIfUnique() {
                return objectStorageService;
            }

            @Override
            public ObjectStorageService getObject() {
                return objectStorageService;
            }
        };
    }

    private static UserProfileRepository userProfileRepository() {
        return new UserProfileRepository() {
            @Override
            public Optional<UserProfile> findByAccountId(long accountId) {
                return Optional.of(new UserProfile(
                        accountId,
                        "carry-user-" + accountId,
                        "avatars/u/" + accountId + ".png",
                        "",
                        0L,
                        0L,
                        Instant.parse("2026-04-20T12:00:00Z"),
                        Instant.parse("2026-04-20T12:00:00Z")
                ));
            }

            @Override
            public java.util.List<UserProfile> findAll() {
                return java.util.List.of();
            }

            @Override
            public java.util.List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
                return java.util.List.of();
            }

            @Override
            public java.util.List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
                return java.util.List.of();
            }

            @Override
            public UserProfile save(UserProfile userProfile) {
                return userProfile;
            }

            @Override
            public UserProfile update(UserProfile userProfile) {
                return userProfile;
            }
        };
    }

}

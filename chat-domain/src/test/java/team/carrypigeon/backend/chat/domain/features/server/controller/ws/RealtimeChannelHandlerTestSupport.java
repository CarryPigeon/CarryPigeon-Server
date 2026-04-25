package team.carrypigeon.backend.chat.domain.features.server.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginConfiguration;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.CustomChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.RealtimeMessageHandlingConfiguration;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeInboundMessageDispatcher;
import team.carrypigeon.backend.chat.domain.features.server.support.realtime.RealtimeSessionRegistry;
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

    static EmbeddedChannel channel(RealtimeSessionRegistry registry, MessageApplicationService service) {
        Supplier<MessageApplicationService> supplier = () -> service;
        MessagePluginConfiguration messagePluginConfiguration = new MessagePluginConfiguration();
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = messagePluginConfiguration.messageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ChannelMessagePluginRegistry pluginRegistry = messagePluginConfiguration.channelMessagePluginRegistry(
                messagePluginConfiguration.textChannelMessagePlugin(),
                messagePluginConfiguration.pluginChannelMessagePlugin(jsonProvider),
                messagePluginConfiguration.customChannelMessagePlugin(jsonProvider),
                messagePluginConfiguration.systemChannelMessagePlugin(jsonProvider),
                new team.carrypigeon.backend.chat.domain.features.message.config.MessagePluginGovernanceProperties(),
                typedObjectProvider(messagePluginConfiguration.fileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)),
                typedObjectProvider(messagePluginConfiguration.voiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy))
        );
        RealtimeMessageHandlingConfiguration realtimeMessageHandlingConfiguration = new RealtimeMessageHandlingConfiguration();
        RealtimeInboundMessageDispatcher dispatcher = realtimeMessageHandlingConfiguration.realtimeInboundMessageDispatcher(
                List.of(
                        realtimeMessageHandlingConfiguration.sendChannelMessageRealtimeHandler(jsonProvider),
                        realtimeMessageHandlingConfiguration.sendFileMessageRealtimeHandler(jsonProvider),
                        realtimeMessageHandlingConfiguration.sendVoiceMessageRealtimeHandler(jsonProvider)
                )
        );
        return new EmbeddedChannel(new RealtimeChannelHandler(
                jsonProvider,
                () -> 9001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                registry,
                supplier,
                () -> dispatcher
        ));
    }

    static MessageApplicationService service(RealtimeSessionRegistry registry) {
        MessagePluginConfiguration messagePluginConfiguration = new MessagePluginConfiguration();
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = messagePluginConfiguration.messageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        ChannelMessagePluginRegistry pluginRegistry = new ChannelMessagePluginRegistry(List.of(
                registration("builtin-text-message", "text", "text", "always_available", messagePluginConfiguration.textChannelMessagePlugin()),
                registration("builtin-plugin-message", "plugin", "plugin", "always_available", messagePluginConfiguration.pluginChannelMessagePlugin(jsonProvider)),
                registration("builtin-custom-message", "custom", "custom", "always_available", messagePluginConfiguration.customChannelMessagePlugin(jsonProvider)),
                registration(
                        "builtin-file-message",
                        "file",
                        "file",
                        "requires_object_storage",
                        messagePluginConfiguration.fileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                ),
                registration(
                        "builtin-voice-message",
                        "voice",
                        "voice",
                        "requires_object_storage",
                        messagePluginConfiguration.voiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                )
        ));
        return new MessageApplicationService(
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
                                1L, 1L, "public", "public", true,
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
                    public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
                    }

                    @Override
                    public List<Long> findAccountIdsByChannelId(long channelId) {
                        return List.of(1001L, 1002L);
                    }
                },
                new ChannelAuditLogRepository() {
                    @Override
                    public void append(ChannelAuditLog channelAuditLog) {
                    }
                },
                new ChannelGovernancePolicy(),
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
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                new MessageRealtimePublisher() {
                    @Override
                    public void publish(ChannelMessage message, java.util.Collection<Long> recipientAccountIds) {
                        String payload = jsonProvider().toJson(new RealtimeServerMessage(
                                "channel_message",
                                null,
                                Instant.parse("2026-04-22T00:00:00Z").toEpochMilli(),
                                message
                        ));
                        for (Long recipientAccountId : recipientAccountIds) {
                            registry.getChannels(recipientAccountId).forEach(channel -> channel.writeAndFlush(new TextWebSocketFrame(payload)));
                        }
                    }

                    @Override
                    public void publishUpdate(ChannelMessage message, java.util.Collection<Long> recipientAccountIds) {
                        publish(message, recipientAccountIds);
                    }
                },
                pluginRegistry,
                objectKeyPolicy,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
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
                },
                objectStorageServiceProvider
        );
    }

    static MessageApplicationService failingService() {
        MessagePluginConfiguration messagePluginConfiguration = new MessagePluginConfiguration();
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = messagePluginConfiguration.messageAttachmentObjectKeyPolicy();
        ObjectStorageService objectStorageService = storageService();
        JsonProvider jsonProvider = jsonProvider();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        return new MessageApplicationService(
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
                                1L, 1L, "public", "public", true,
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
                    public void save(team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember channelMember) {
                    }

                    @Override
                    public List<Long> findAccountIdsByChannelId(long channelId) {
                        return List.of(1001L);
                    }
                },
                new ChannelAuditLogRepository() {
                    @Override
                    public void append(ChannelAuditLog channelAuditLog) {
                    }
                },
                new ChannelGovernancePolicy(),
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
                    public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
                        return List.of();
                    }

                    @Override
                    public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
                        return List.of();
                    }
                },
                (message, recipientAccountIds) -> {
                },
                new ChannelMessagePluginRegistry(List.of(
                        registration("builtin-text-message", "text", "text", "always_available", messagePluginConfiguration.textChannelMessagePlugin()),
                        registration("builtin-plugin-message", "plugin", "plugin", "always_available", messagePluginConfiguration.pluginChannelMessagePlugin(jsonProvider)),
                        registration("builtin-custom-message", "custom", "custom", "always_available", messagePluginConfiguration.customChannelMessagePlugin(jsonProvider)),
                        registration(
                                "builtin-file-message",
                                "file",
                                "file",
                                "requires_object_storage",
                                messagePluginConfiguration.fileChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                        ),
                        registration(
                                "builtin-voice-message",
                                "voice",
                                "voice",
                                "requires_object_storage",
                                messagePluginConfiguration.voiceChannelMessagePlugin(objectStorageService, jsonProvider, objectKeyPolicy)
                        )
                )),
                objectKeyPolicy,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
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
                },
                objectStorageServiceProvider
        );
    }

    static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
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

    private static <T> ObjectProvider<T> typedObjectProvider(T object) {
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
}

package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SendChannelMessageRealtimeHandler 契约测试。
 * 职责：验证统一 realtime send handler 对 text/file/voice/扩展类型的路由和委托行为。
 * 边界：不验证完整 WebSocket 协议处理链，只验证统一 handler 自身语义。
 */
@Tag("contract")
class SendChannelMessageRealtimeHandlerTests {

    /**
     * 验证 text 类型请求会被统一 handler 接收并成功委托发送。
     */
    @Test
    @DisplayName("supports text message and delegates send")
    void supports_textMessage_andDelegatesSend() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        RecordingMessageRepository messageRepository = new RecordingMessageRepository();
        MessageApplicationService service = service(messageRepository);
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "text",
                "hello world",
                null,
                null
        );

        assertTrue(handler.supports(request));
        handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service);

        assertEquals("text", messageRepository.savedMessage.messageType());
        assertEquals("hello world", messageRepository.savedMessage.body());
    }

    /**
     * 验证 file 类型请求会被统一 handler 接收并成功委托发送。
     */
    @Test
    @DisplayName("supports file message and delegates send")
    void supports_fileMessage_andDelegatesSend() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        RecordingMessageRepository messageRepository = new RecordingMessageRepository();
        MessageApplicationService service = service(messageRepository);
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "file",
                "项目文档",
                Map.of(
                        "object_key", "channels/1/messages/file/accounts/1001/5001-demo.pdf",
                        "filename", "demo.pdf",
                        "mime_type", "application/pdf",
                        "size", 12345
                ),
                null
        );

        assertTrue(handler.supports(request));
        handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service);

        assertEquals("file", messageRepository.savedMessage.messageType());
        assertEquals("[文件消息] demo.pdf", messageRepository.savedMessage.previewText());
    }

    /**
     * 验证 voice 类型请求会被统一 handler 接收并成功委托发送。
     */
    @Test
    @DisplayName("supports voice message and delegates send")
    void supports_voiceMessage_andDelegatesSend() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        RecordingMessageRepository messageRepository = new RecordingMessageRepository();
        MessageApplicationService service = service(messageRepository);
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "voice",
                null,
                Map.of(
                        "object_key", "channels/1/messages/voice/accounts/1001/5001-demo.mp3",
                        "filename", "demo.mp3",
                        "mime_type", "audio/mpeg",
                        "size", 45678,
                        "duration_millis", 12000,
                        "transcript", "会议纪要"
                ),
                null
        );

        assertTrue(handler.supports(request));
        handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service);

        assertEquals("voice", messageRepository.savedMessage.messageType());
        assertEquals("[语音消息] demo.mp3 12s", messageRepository.savedMessage.previewText());
    }

    /**
     * 验证非内建类型会走 plugin-style 扩展路径。
     */
    @Test
    @DisplayName("supports extension message and delegates through plugin style send")
    void supports_extensionMessage_andDelegatesThroughPluginStyleSend() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        RecordingMessageRepository messageRepository = new RecordingMessageRepository();
        MessageApplicationService service = service(messageRepository);
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "test-extension",
                "player joined",
                Map.of("event", "player_join"),
                null
        );

        assertTrue(handler.supports(request));
        handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service);

        assertEquals("test-extension", messageRepository.savedMessage.messageType());
        assertEquals("[插件消息] player joined", messageRepository.savedMessage.previewText());
    }

    /**
     * 验证未注册扩展消息类型会被稳定拒绝。
     */
    @Test
    @DisplayName("handle unregistered extension message throws validation problem")
    void handle_unregisteredExtensionMessage_throwsValidationProblem() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        MessageApplicationService service = serviceWithoutExtensionRegistration(new RecordingMessageRepository());
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "test-extension",
                "player joined",
                Map.of("event", "player_join"),
                null
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service)
        );

        assertEquals("unsupported extension message type", exception.getMessage());
    }

    /**
     * 验证扩展消息缺少 payload 时返回稳定校验问题。
     */
    @Test
    @DisplayName("handle extension message without payload throws validation problem")
    void handle_extensionMessageWithoutPayload_throwsValidationProblem() {
        SendChannelMessageRealtimeHandler handler = new SendChannelMessageRealtimeHandler(jsonProvider());
        MessageApplicationService service = service(new RecordingMessageRepository());
        RealtimeClientMessage request = new RealtimeClientMessage(
                "send_channel_message",
                1L,
                "test-extension",
                "player joined",
                null,
                null
        );

        ProblemException exception = assertThrows(
                ProblemException.class,
                () -> handler.handle(new AuthenticatedPrincipal(1001L, "carry-user"), request, service)
        );

        assertEquals("payload must not be blank", exception.getMessage());
    }

    private static MessageApplicationService service(RecordingMessageRepository messageRepository) {
        JsonProvider jsonProvider = jsonProvider();
        ObjectStorageService objectStorageService = storageService();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
        return new MessageApplicationService(
                channelRepository(),
                channelMemberRepository(),
                new ChannelAuditLogRepository() {
                    @Override
                    public void append(ChannelAuditLog channelAuditLog) {
                    }
                },
                new ChannelGovernancePolicy(),
                messageRepository,
                (message, recipients) -> {
                },
                new ChannelMessagePluginRegistry(List.of(
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
                )),
                objectKeyPolicy,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new NoopTransactionRunner(),
                objectStorageServiceProvider
        );
    }

    private static MessageApplicationService serviceWithoutExtensionRegistration(RecordingMessageRepository messageRepository) {
        JsonProvider jsonProvider = jsonProvider();
        ObjectStorageService objectStorageService = storageService();
        ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(objectStorageService);
        MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
        return new MessageApplicationService(
                channelRepository(),
                channelMemberRepository(),
                new ChannelAuditLogRepository() {
                    @Override
                    public void append(ChannelAuditLog channelAuditLog) {
                    }
                },
                new ChannelGovernancePolicy(),
                messageRepository,
                (message, recipients) -> {
                },
                new ChannelMessagePluginRegistry(List.of(
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
                )),
                objectKeyPolicy,
                new MessageAttachmentPayloadResolver(objectStorageServiceProvider, jsonProvider),
                new ServerIdentityProperties("carrypigeon-local"),
                () -> 7001L,
                new TimeProvider(Clock.fixed(Instant.parse("2026-04-22T00:00:00Z"), ZoneOffset.UTC)),
                new NoopTransactionRunner(),
                objectStorageServiceProvider
        );
    }

    private static JsonProvider jsonProvider() {
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
            public java.util.Optional<StorageObject> get(GetObjectCommand command) {
                if (command.objectKey().endsWith(".mp3")) {
                    return java.util.Optional.of(StorageObject.metadata(command.objectKey(), "audio/mpeg", 45678L));
                }
                return java.util.Optional.of(StorageObject.metadata(command.objectKey(), "application/pdf", 12345L));
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

    private static ChannelRepository channelRepository() {
        return new ChannelRepository() {
            @Override
            public java.util.Optional<Channel> findDefaultChannel() {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<Channel> findSystemChannel() {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<Channel> findById(long channelId) {
                return java.util.Optional.of(new Channel(
                        1L, 1L, "public", "public", true,
                        Instant.parse("2026-04-22T00:00:00Z"), Instant.parse("2026-04-22T00:00:00Z")
                ));
            }
        };
    }

    private static ChannelMemberRepository channelMemberRepository() {
        return new ChannelMemberRepository() {
            @Override
            public boolean exists(long channelId, long accountId) {
                return true;
            }

            @Override
            public java.util.Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
                return java.util.Optional.of(new ChannelMember(
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
        };
    }

    private static class RecordingMessageRepository implements MessageRepository {

        private ChannelMessage savedMessage;

        @Override
        public ChannelMessage save(ChannelMessage message) {
            this.savedMessage = message;
            return message;
        }

        @Override
        public java.util.Optional<ChannelMessage> findById(long messageId) {
            return java.util.Optional.ofNullable(savedMessage)
                    .filter(message -> message.messageId() == messageId);
        }

        @Override
        public ChannelMessage update(ChannelMessage message) {
            this.savedMessage = message;
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
    }

    private static class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }
}

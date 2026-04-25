package team.carrypigeon.backend.chat.domain.features.server.support.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.server.controller.ws.RealtimeClientMessage;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SendFileMessageRealtimeHandler 契约测试。
 * 职责：验证文件消息 realtime handler 的路由和委托发送行为。
 * 边界：不验证完整 WebSocket 协议处理链，只验证 handler 自身语义。
 */
@Tag("contract")
class SendFileMessageRealtimeHandlerTests {

    /**
     * 验证 file 类型请求会被当前 handler 接收并成功委托发送。
     */
    @Test
    @DisplayName("supports file message and delegates send")
    void supports_fileMessage_andDelegatesSend() {
        SendFileMessageRealtimeHandler handler = new SendFileMessageRealtimeHandler(jsonProvider());
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

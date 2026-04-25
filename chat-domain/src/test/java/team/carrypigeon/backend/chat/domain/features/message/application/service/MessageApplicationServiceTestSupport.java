package team.carrypigeon.backend.chat.domain.features.message.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMemberRole;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
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
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.SystemChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.DeleteObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.GetObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrl;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PresignedUrlCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * MessageApplicationService 测试支持。
 * 职责：为发送、附件和查询拆分后的契约测试提供共享 fixture 与内存替身。
 * 边界：只服务 message application service 测试，不扩展为通用测试基类体系。
 */
final class MessageApplicationServiceTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");

    private MessageApplicationServiceTestSupport() {
    }

    static final class Fixture {

        final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        final InMemoryChannelAuditLogRepository channelAuditLogRepository = new InMemoryChannelAuditLogRepository();
        final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        final RecordingMessageRealtimePublisher publisher = new RecordingMessageRealtimePublisher();
        final JsonProvider jsonProvider = jsonProvider();
        final MessageApplicationService service;

        Fixture(ObjectStorageService storageService) {
            channelRepository.channel = new Channel(1L, 1L, "public", "public", true, BASE_TIME, BASE_TIME);
            channelMemberRepository.save(new ChannelMember(1L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
            channelMemberRepository.save(new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(1), null));
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(storageService);
            MessageAttachmentPayloadResolver payloadResolver = new MessageAttachmentPayloadResolver(
                    objectStorageServiceProvider,
                    jsonProvider
            );
            MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
            this.service = new MessageApplicationService(
                    channelRepository,
                    channelMemberRepository,
                    channelAuditLogRepository,
                    new ChannelGovernancePolicy(),
                    messageRepository,
                    publisher,
                    channelMessagePluginRegistry(storageService, jsonProvider, objectKeyPolicy),
                    objectKeyPolicy,
                    payloadResolver,
                    new ServerIdentityProperties("carrypigeon-local"),
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    new NoopTransactionRunner(),
                    objectStorageServiceProvider
            );
        }
    }

    static ChannelMessagePluginRegistry channelMessagePluginRegistry(
            ObjectStorageService storageService,
            JsonProvider jsonProvider,
            MessageAttachmentObjectKeyPolicy objectKeyPolicy
    ) {
        List<ChannelMessagePluginRegistration> registrations = new ArrayList<>();
        registrations.add(registration(
                "builtin-text-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-text-message",
                        "text",
                        "text",
                        "Built-in text channel message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:text:send"),
                        "always_available"
                ),
                new TextChannelMessagePlugin()
        ));
        registrations.add(registration(
                "builtin-plugin-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-plugin-message",
                        "plugin",
                        "plugin",
                        "Built-in plugin channel message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:plugin:send"),
                        "always_available"
                ),
                new PluginChannelMessagePlugin(jsonProvider)
        ));
        registrations.add(registration(
                "builtin-custom-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-custom-message",
                        "custom",
                        "custom",
                        "Built-in custom channel message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:custom:send"),
                        "always_available"
                ),
                new CustomChannelMessagePlugin(jsonProvider)
        ));
        registrations.add(registration(
                "builtin-system-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-system-message",
                        "system",
                        "system",
                        "Built-in system channel message plugin",
                        false,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:system:send"),
                        "internal_only"
                ),
                new SystemChannelMessagePlugin(jsonProvider)
        ));
        if (storageService != null) {
            registrations.add(registration(
                    "builtin-file-message",
                    new ChannelMessagePluginDescriptor(
                            "builtin-file-message",
                            "file",
                            "file",
                            "Built-in file channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:file:send"),
                            "requires_object_storage"
                    ),
                    new FileChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy)
            ));
            registrations.add(registration(
                    "builtin-voice-message",
                    new ChannelMessagePluginDescriptor(
                            "builtin-voice-message",
                            "voice",
                            "voice",
                            "Built-in voice channel message plugin",
                            true,
                            List.of("message.sent", "message.recalled"),
                            List.of("message:voice:send"),
                            "requires_object_storage"
                    ),
                    new VoiceChannelMessagePlugin(storageService, jsonProvider, objectKeyPolicy)
            ));
        }
        return new ChannelMessagePluginRegistry(registrations);
    }

    private static ChannelMessagePluginRegistration registration(
            String pluginKey,
            ChannelMessagePluginDescriptor descriptor,
            ChannelMessagePlugin plugin
    ) {
        return new ChannelMessagePluginRegistration(descriptor, plugin);
    }

    static JsonProvider jsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JsonProvider(objectMapper);
    }

    static ObjectProvider<ObjectStorageService> objectProvider(ObjectStorageService storageService) {
        return new ObjectProvider<>() {
            @Override
            public ObjectStorageService getObject(Object... args) {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfAvailable() {
                return storageService;
            }

            @Override
            public ObjectStorageService getIfUnique() {
                return storageService;
            }

            @Override
            public ObjectStorageService getObject() {
                return storageService;
            }
        };
    }

    static final class InMemoryChannelRepository implements ChannelRepository {

        Channel channel;

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channel);
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return Optional.ofNullable(channel != null && "system".equals(channel.type()) ? channel : null);
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channel != null && channel.id() == channelId ? channel : null);
        }
    }

    static final class InMemoryChannelMemberRepository implements ChannelMemberRepository {

        final Map<Long, List<ChannelMember>> memberships = new HashMap<>();

        @Override
        public boolean exists(long channelId, long accountId) {
            return memberships.getOrDefault(channelId, List.of()).stream().anyMatch(member -> member.accountId() == accountId);
        }

        @Override
        public void save(ChannelMember channelMember) {
            memberships.computeIfAbsent(channelMember.channelId(), ignored -> new ArrayList<>()).add(channelMember);
        }

        @Override
        public Optional<ChannelMember> findByChannelIdAndAccountId(long channelId, long accountId) {
            return memberships.getOrDefault(channelId, List.of()).stream()
                    .filter(member -> member.accountId() == accountId)
                    .findFirst();
        }

        @Override
        public void update(ChannelMember channelMember) {
            List<ChannelMember> members = new ArrayList<>(memberships.getOrDefault(channelMember.channelId(), List.of()));
            members.removeIf(existing -> existing.accountId() == channelMember.accountId());
            members.add(channelMember);
            memberships.put(channelMember.channelId(), members);
        }

        @Override
        public void delete(long channelId, long accountId) {
            List<ChannelMember> members = new ArrayList<>(memberships.getOrDefault(channelId, List.of()));
            members.removeIf(member -> member.accountId() == accountId);
            memberships.put(channelId, members);
        }

        @Override
        public List<Long> findAccountIdsByChannelId(long channelId) {
            return memberships.getOrDefault(channelId, List.of()).stream()
                    .map(ChannelMember::accountId)
                    .toList();
        }
    }

    static final class InMemoryMessageRepository implements MessageRepository {

        final List<ChannelMessage> savedMessages = new ArrayList<>();
        final List<ChannelMessage> updatedMessages = new ArrayList<>();
        final List<ChannelMessage> history = new ArrayList<>();
        final List<ChannelMessage> searchResults = new ArrayList<>();
        final Map<Long, ChannelMessage> messagesById = new HashMap<>();

        @Override
        public ChannelMessage save(ChannelMessage message) {
            savedMessages.add(message);
            messagesById.put(message.messageId(), message);
            return message;
        }

        @Override
        public Optional<ChannelMessage> findById(long messageId) {
            return Optional.ofNullable(messagesById.get(messageId));
        }

        @Override
        public ChannelMessage update(ChannelMessage message) {
            updatedMessages.add(message);
            messagesById.put(message.messageId(), message);
            replaceMessage(history, message);
            replaceMessage(searchResults, message);
            return message;
        }

        @Override
        public List<ChannelMessage> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return history;
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return searchResults;
        }

        private void replaceMessage(List<ChannelMessage> messages, ChannelMessage updatedMessage) {
            for (int index = 0; index < messages.size(); index++) {
                if (messages.get(index).messageId() == updatedMessage.messageId()) {
                    messages.set(index, updatedMessage);
                    return;
                }
            }
        }
    }

    static final class InMemoryChannelAuditLogRepository implements ChannelAuditLogRepository {

        final List<ChannelAuditLog> logs = new ArrayList<>();

        @Override
        public void append(ChannelAuditLog channelAuditLog) {
            logs.add(channelAuditLog);
        }
    }

    static final class RecordingMessageRealtimePublisher implements MessageRealtimePublisher {

        final List<ChannelMessage> publishedMessages = new ArrayList<>();
        final List<List<Long>> recipientAccountIds = new ArrayList<>();

        @Override
        public void publish(ChannelMessage message, java.util.Collection<Long> recipients) {
            publishedMessages.add(message);
            recipientAccountIds.add(List.copyOf(recipients));
        }
    }

    static final class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 5001L;
        }
    }

    static final class NoopTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    static final class TestObjectStorageService implements ObjectStorageService {

        StorageObject putResult = StorageObject.metadata("channels/1/messages/file/accounts/1001/5001-demo.pdf", "application/pdf", 128L);
        Optional<StorageObject> getResult = Optional.empty();
        PresignedUrl presignedUrl = new PresignedUrl(URI.create("http://127.0.0.1:9000/default"), BASE_TIME.plusSeconds(1800));
        PutObjectCommand lastPutCommand;

        @Override
        public StorageObject put(PutObjectCommand command) {
            this.lastPutCommand = command;
            return putResult;
        }

        @Override
        public Optional<StorageObject> get(GetObjectCommand command) {
            return getResult;
        }

        @Override
        public void delete(DeleteObjectCommand command) {
        }

        @Override
        public PresignedUrl createPresignedUrl(PresignedUrlCommand command) {
            return presignedUrl;
        }
    }
}

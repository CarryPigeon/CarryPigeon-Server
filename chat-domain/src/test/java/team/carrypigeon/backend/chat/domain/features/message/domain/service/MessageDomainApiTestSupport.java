package team.carrypigeon.backend.chat.domain.features.message.domain.service;

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
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.support.channel.ChannelBackedMessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.CustomChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.SystemChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.features.user.domain.model.UserProfile;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
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
 * 消息领域 API 测试支撑。
 * 职责：为消息、附件和置顶 API 契约测试提供共享 fixture 与内存替身。
 * 边界：只服务 message domain API 测试，不扩展为通用测试基类体系。
 */
final class MessageDomainApiTestSupport {

    static final Instant BASE_TIME = Instant.parse("2026-04-22T00:00:00Z");
    static final String SERVER_ID = "550e8400-e29b-41d4-a716-446655440000";

    private MessageDomainApiTestSupport() {
    }

    /**
     * `Fixture` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class Fixture {

        final InMemoryChannelRepository channelRepository = new InMemoryChannelRepository();
        final InMemoryChannelMemberRepository channelMemberRepository = new InMemoryChannelMemberRepository();
        final InMemoryChannelAuditLogRepository channelAuditLogRepository = new InMemoryChannelAuditLogRepository();
        final InMemoryChannelPinRepository channelPinRepository = new InMemoryChannelPinRepository();
        final InMemoryMessageRepository messageRepository = new InMemoryMessageRepository();
        final InMemoryMentionRepository mentionRepository = new InMemoryMentionRepository();
        final RecordingMessageRealtimePublisher publisher = new RecordingMessageRealtimePublisher();
        final JsonProvider jsonProvider = jsonProvider();
        final ChannelMessagePublishingDomainApi publishingApi;
        final ChannelMessageLifecycleDomainApi lifecycleApi;
        final ChannelMessageTimelineDomainApi timelineApi;
        final ChannelMessageAttachmentDomainApi attachmentApi;
        final ChannelPinDomainApi pinApi;

        Fixture(ObjectStorageService storageService) {
            this(storageService, new NoopTransactionRunner());
        }

        Fixture(ObjectStorageService storageService, TransactionRunner transactionRunner) {
            channelRepository.channels.put(1L, new Channel(1L, 1L, "public", "", "", "", "public", true, BASE_TIME, BASE_TIME));
            channelMemberRepository.save(new ChannelMember(1L, 1001L, ChannelMemberRole.MEMBER, BASE_TIME, null));
            channelMemberRepository.save(new ChannelMember(1L, 1002L, ChannelMemberRole.MEMBER, BASE_TIME.plusSeconds(1), null));
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider = objectProvider(storageService);
            MessageAttachmentPayloadResolver payloadResolver = new MessageAttachmentPayloadResolver(
                    objectStorageServiceProvider,
                    jsonProvider
            );
            MessageAttachmentObjectKeyPolicy objectKeyPolicy = new MessageAttachmentObjectKeyPolicy();
            ChannelMessagePluginRegistry pluginRegistry = channelMessagePluginRegistry(storageService, jsonProvider, objectKeyPolicy);
            MessageChannelBoundary messageChannelBoundary = new ChannelBackedMessageChannelBoundary(
                    channelRepository,
                    channelMemberRepository,
                    channelAuditLogRepository,
                    channelPinRepository,
                    new ChannelGovernancePolicy()
            );
            this.publishingApi = new ChannelMessagePublishingDomainApi(
                    messageChannelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository(),
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    new FixedIdGenerator(),
                    jsonProvider,
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.lifecycleApi = new ChannelMessageLifecycleDomainApi(
                    messageChannelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository(),
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    new FixedIdGenerator(),
                    jsonProvider,
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.timelineApi = new ChannelMessageTimelineDomainApi(
                    messageChannelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository(),
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    new FixedIdGenerator(),
                    jsonProvider,
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.attachmentApi = new ChannelMessageAttachmentDomainApi(
                    messageChannelBoundary,
                    objectKeyPolicy,
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    objectStorageServiceProvider
            );
            this.pinApi = new ChannelPinDomainApi(
                    messageChannelBoundary,
                    messageRepository,
                    mentionRepository,
                    userProfileRepository(),
                    publisher,
                    pluginRegistry,
                    payloadResolver,
                    new ServerIdentityProperties(SERVER_ID),
                    new FixedIdGenerator(),
                    jsonProvider,
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
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
                "builtin-test-extension-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-test-extension-message",
                        "test-extension",
                        "test-extension",
                        "Built-in test extension message plugin",
                        true,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:test-extension:send"),
                        "always_available"
                ),
                new PluginChannelMessagePlugin("test-extension", jsonProvider)
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

    /**
     * `InMemoryChannelRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryChannelRepository implements ChannelRepository {

        final Map<Long, Channel> channels = new HashMap<>();

        @Override
        public Optional<Channel> findDefaultChannel() {
            return Optional.ofNullable(channels.get(1L));
        }

        @Override
        public Optional<Channel> findSystemChannel() {
            return channels.values().stream().filter(channel -> "system".equals(channel.type())).findFirst();
        }

        @Override
        public Optional<Channel> findById(long channelId) {
            return Optional.ofNullable(channels.get(channelId));
        }
    }

    /**
     * `InMemoryChannelMemberRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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

    /**
     * `InMemoryChannelPinRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryChannelPinRepository implements ChannelPinRepository {

        final List<ChannelPin> pins = new ArrayList<>();

        @Override
        public Optional<ChannelPin> findByChannelIdAndMessageId(long channelId, long messageId) {
            return pins.stream().filter(pin -> pin.channelId() == channelId && pin.messageId() == messageId).findFirst();
        }

        @Override
        public void save(ChannelPin channelPin) {
            pins.removeIf(pin -> pin.channelId() == channelPin.channelId() && pin.messageId() == channelPin.messageId());
            pins.add(channelPin);
        }

        @Override
        public void delete(long channelId, long messageId) {
            pins.removeIf(pin -> pin.channelId() == channelId && pin.messageId() == messageId);
        }

        @Override
        public List<ChannelPin> findByChannelIdBefore(long channelId, Long cursorMessageId, int limit) {
            return pins.stream()
                    .filter(pin -> pin.channelId() == channelId)
                    .filter(pin -> cursorMessageId == null || pin.messageId() < cursorMessageId)
                    .sorted(java.util.Comparator.comparingLong(ChannelPin::messageId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countByChannelId(long channelId) {
            return pins.stream().filter(pin -> pin.channelId() == channelId).count();
        }
    }

    /**
     * `InMemoryMentionRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryMentionRepository implements MentionRepository {

        final List<Mention> mentions = new ArrayList<>();
        Integer failOnSaveCall;
        private int saveCalls;

        @Override
        public void save(Mention mention) {
            saveCalls++;
            if (failOnSaveCall != null && saveCalls == failOnSaveCall) {
                throw new IllegalStateException("mention persistence failed");
            }
            mentions.add(mention);
        }

        @Override
        public List<Mention> listByAccountId(long accountId, Long cursorMentionId, int limit, boolean unreadOnly, Long channelId) {
            return mentions.stream()
                    .filter(mention -> mention.targetAccountId() == accountId)
                    .filter(mention -> cursorMentionId == null || mention.mentionId() < cursorMentionId)
                    .filter(mention -> !unreadOnly || !mention.read())
                    .filter(mention -> channelId == null || mention.channelId() == channelId)
                    .limit(limit)
                    .toList();
        }

        @Override
        public boolean markAsRead(long accountId, long mentionId) {
            return false;
        }

        @Override
        public int markAllAsRead(long accountId, Long beforeMentionId, Long channelId) {
            return 0;
        }
    }

    /**
     * `InMemoryMessageRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryMessageRepository implements MessageRepository {

        final List<ChannelMessage> savedMessages = new ArrayList<>();
        final List<ChannelMessage> updatedMessages = new ArrayList<>();
        final List<ChannelMessage> history = new ArrayList<>();
        final List<ChannelMessage> searchResults = new ArrayList<>();
        Long lastSearchCursorMessageId;
        Long lastSearchSenderAccountId;
        String lastSearchDomain;
        Long lastSearchBeforeMessageId;
        Long lastSearchAfterMessageId;
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
            return history.stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> cursorMessageId == null || message.messageId() < cursorMessageId)
                    .sorted(java.util.Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ChannelMessage> findByChannelIdAfter(long channelId, long afterMessageId, int limit) {
            return history.stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> message.messageId() > afterMessageId)
                    .sorted(java.util.Comparator.comparingLong(ChannelMessage::messageId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ChannelMessage> searchByChannelId(long channelId, String keyword, int limit) {
            return searchByChannelId(channelId, keyword, null, null, null, null, null, limit);
        }

        @Override
        public List<ChannelMessage> searchByChannelId(
                long channelId,
                String keyword,
                Long cursorMessageId,
                Long senderAccountId,
                String domain,
                Long beforeMessageId,
                Long afterMessageId,
                int limit
        ) {
            lastSearchCursorMessageId = cursorMessageId;
            lastSearchSenderAccountId = senderAccountId;
            lastSearchDomain = domain;
            lastSearchBeforeMessageId = beforeMessageId;
            lastSearchAfterMessageId = afterMessageId;
            return searchResults.stream()
                    .filter(message -> message.channelId() == channelId)
                    .filter(message -> cursorMessageId == null || message.messageId() < cursorMessageId)
                    .filter(message -> senderAccountId == null || message.senderId() == senderAccountId)
                    .filter(message -> domain == null || domain.equals(message.messageType()))
                    .filter(message -> beforeMessageId == null || message.messageId() < beforeMessageId)
                    .filter(message -> afterMessageId == null || message.messageId() > afterMessageId)
                    .filter(message -> message.searchableText() != null && message.searchableText().contains(keyword.trim()))
                    .sorted(java.util.Comparator.comparingLong(ChannelMessage::messageId).reversed())
                    .limit(limit)
                    .toList();
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

    /**
     * `InMemoryChannelAuditLogRepository` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class InMemoryChannelAuditLogRepository implements ChannelAuditLogRepository {

        final List<ChannelAuditLog> logs = new ArrayList<>();

        @Override
        public void append(ChannelAuditLog channelAuditLog) {
            logs.add(channelAuditLog);
        }
    }

    /**
     * `RecordingMessageRealtimePublisher` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class RecordingMessageRealtimePublisher implements MessageRealtimePublisher {

        final List<ChannelMessage> publishedMessages = new ArrayList<>();
        final List<MessageSenderSnapshot> senderSnapshots = new ArrayList<>();
        final List<List<Long>> recipientAccountIds = new ArrayList<>();
        final List<MessageChannelBoundary.MessageChannelPin> pinnedMessages = new ArrayList<>();
        final List<MessageChannelBoundary.MessageChannelPin> unpinnedMessages = new ArrayList<>();
        final List<Mention> createdMentions = new ArrayList<>();

        @Override
        public void publish(ChannelMessage message, MessageSenderSnapshot senderSnapshot, java.util.Collection<Long> recipients) {
            publishedMessages.add(message);
            senderSnapshots.add(senderSnapshot);
            recipientAccountIds.add(List.copyOf(recipients));
        }

        @Override
        public void publishPin(MessageChannelBoundary.MessageChannelPin pin, java.util.Collection<Long> recipients) {
            pinnedMessages.add(pin);
        }

        @Override
        public void publishUnpin(MessageChannelBoundary.MessageChannelPin pin, long unpinnedByAccountId, long unpinnedAt, java.util.Collection<Long> recipients) {
            unpinnedMessages.add(pin);
        }

        @Override
        public void publishMentionCreated(Mention mention, java.util.Collection<Long> recipientAccountIds) {
            createdMentions.add(mention);
        }
    }

    /**
     * `FixedIdGenerator` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class FixedIdGenerator implements IdGenerator {

        @Override
        public long nextLongId() {
            return 5001L;
        }
    }

    /**
     * `NoopTransactionRunner` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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

    /**
     * `RollbackingTransactionRunner` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class RollbackingTransactionRunner implements TransactionRunner {

        @Override
        public <T> T runInTransaction(java.util.function.Supplier<T> action) {
            action.get();
            throw new IllegalStateException("transaction rolled back");
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
            throw new IllegalStateException("transaction rolled back");
        }
    }

    /**
     * `TestObjectStorageService` 测试辅助类型。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
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
                        BASE_TIME,
                        BASE_TIME
                ));
            }

            @Override
            public List<UserProfile> findAll() {
                return List.of();
            }

            @Override
            public List<UserProfile> findByAccountIdBefore(Long cursorAccountId, int limit) {
                return List.of();
            }

            @Override
            public List<UserProfile> searchByKeyword(String keyword, Long cursorAccountId, int limit) {
                return List.of();
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

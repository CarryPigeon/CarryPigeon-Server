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
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelMessagingDomainApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageIdempotency;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginDescriptor;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistration;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.service.MessageDomainPluginDomainApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.CustomChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.FileChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.ForwardChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.PluginChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.ReplyTextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.SystemChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.TextChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.plugin.support.message.VoiceChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
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
import team.carrypigeon.backend.chat.domain.support.TestFeatureApis;

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
        final InMemoryMessageIdempotencyRepository messageIdempotencyRepository = new InMemoryMessageIdempotencyRepository();
        final RecordingRealtimeEventApi publisher = new RecordingRealtimeEventApi();
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
            ChannelMessagePluginRegistry pluginRegistry = channelMessagePluginRegistry(storageService);
            ChannelMessagingApi channelMessagingApi = new ChannelMessagingDomainApi(
                    channelRepository,
                    channelMemberRepository,
                    channelAuditLogRepository,
                    channelPinRepository,
                    new ChannelGovernancePolicy()
            );
            this.publishingApi = new ChannelMessagePublishingDomainApi(
                    channelMessagingApi,
                    messageRepository,
                    mentionRepository,
                    messageIdempotencyRepository,
                    publisher,
                    new MessageDomainPluginDomainApi(pluginRegistry),
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.lifecycleApi = new ChannelMessageLifecycleDomainApi(
                    channelMessagingApi,
                    messageRepository,
                    mentionRepository,
                    publisher,
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.timelineApi = new ChannelMessageTimelineDomainApi(
                    channelMessagingApi,
                    messageRepository,
                    mentionRepository,
                    publisher,
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
            this.attachmentApi = new ChannelMessageAttachmentDomainApi(
                    channelMessagingApi,
                    TestFeatureApis.fileReferences(),
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    objectStorageServiceProvider
            );
            this.pinApi = new ChannelPinDomainApi(
                    channelMessagingApi,
                    messageRepository,
                    mentionRepository,
                    publisher,
                    new FixedIdGenerator(),
                    new TimeProvider(Clock.fixed(BASE_TIME, ZoneOffset.UTC)),
                    transactionRunner
            );
        }
    }

    static ChannelMessagePluginRegistry channelMessagePluginRegistry(ObjectStorageService storageService) {
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
                "builtin-reply-text-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-reply-text-message",
                        "reply-text",
                        "reply-text",
                        "Built-in reply text channel message plugin",
                        false,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:reply-text:send"),
                        "always_available"
                ),
                new ReplyTextChannelMessagePlugin()
        ));
        registrations.add(registration(
                "builtin-forward-message",
                new ChannelMessagePluginDescriptor(
                        "builtin-forward-message",
                        "forward",
                        "forward",
                        "Built-in forward channel message plugin",
                        false,
                        List.of("message.sent", "message.recalled"),
                        List.of("message:forward:send"),
                        "forward_endpoint_only"
                ),
                new ForwardChannelMessagePlugin()
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
                new PluginChannelMessagePlugin("test-extension")
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
                new CustomChannelMessagePlugin()
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
                new SystemChannelMessagePlugin()
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
                    new FileChannelMessagePlugin(storageService, TestFeatureApis.fileReferences())
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
                    new VoiceChannelMessagePlugin(storageService, TestFeatureApis.fileReferences())
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
        public void deleteByMessageId(long messageId) {
            mentions.removeIf(mention -> mention.messageId() == messageId);
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
                    .filter(message -> domain == null || domain.equals(message.domain()))
                    .filter(message -> beforeMessageId == null || message.messageId() < beforeMessageId)
                    .filter(message -> afterMessageId == null || message.messageId() > afterMessageId)
                    .filter(message -> message.status() == team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus.SENT)
                    .filter(message -> message.preview().contains(keyword.trim()) || message.data().toString().contains(keyword.trim()))
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
     * `InMemoryMessageIdempotencyRepository` 测试替身。
     * 职责：记录幂等预留与结果，支持领域服务的重复请求契约测试。
     */
    static final class InMemoryMessageIdempotencyRepository implements MessageIdempotencyRepository {

        final Map<String, MessageIdempotency> reservations = new HashMap<>();
        boolean failOnComplete;

        @Override
        public MessageIdempotency reserve(MessageIdempotency reservation) {
            return reservations.computeIfAbsent(key(reservation.accountId(), reservation.operation(), reservation.idempotencyKey()),
                    ignored -> reservation);
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
            if (failOnComplete) {
                throw new IllegalStateException("idempotency completion failed");
            }
            String key = key(accountId, operation, idempotencyKey);
            MessageIdempotency existing = reservations.get(key);
            reservations.put(key, new MessageIdempotency(
                    accountId,
                    operation,
                    idempotencyKey,
                    existing.requestFingerprint(),
                    messageId,
                    existing.createdAt(),
                    completedAt
            ));
        }

        private String key(long accountId, String operation, String idempotencyKey) {
            return accountId + "\n" + operation + "\n" + idempotencyKey;
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
     * `RecordingRealtimeEventApi` 测试替身。
     * 职责：隔离外部依赖，使测试只验证当前契约边界。
     */
    static final class RecordingRealtimeEventApi implements RealtimeEventApi {

        final List<ChannelMessage> publishedMessages = new ArrayList<>();
        final List<List<Long>> recipientAccountIds = new ArrayList<>();
        final List<ChannelPinReference> pinnedMessages = new ArrayList<>();
        final List<ChannelPinReference> unpinnedMessages = new ArrayList<>();
        final List<Mention> createdMentions = new ArrayList<>();

        @Override
        public void publish(PublishRealtimeEventCommand command) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) command.payload();
            switch (command.eventType()) {
                case "message.created" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) payload.get("message");
                    publishedMessages.add(message(
                            Long.parseLong(String.valueOf(message.get("mid"))),
                            Long.parseLong(String.valueOf(payload.get("cid"))),
                            "sent"
                    ));
                    recipientAccountIds.add(List.copyOf(command.recipientAccountIds()));
                }
                case "message.recalled" -> {
                    publishedMessages.add(message(
                            Long.parseLong(String.valueOf(payload.get("mid"))),
                            Long.parseLong(String.valueOf(payload.get("cid"))),
                            "recalled"
                    ));
                    recipientAccountIds.add(List.copyOf(command.recipientAccountIds()));
                }
                case "message.pinned" -> pinnedMessages.add(pin(payload));
                case "message.unpinned" -> unpinnedMessages.add(pin(payload));
                case "mention.created" -> {
                    createdMentions.add(new Mention(
                            Long.parseLong(String.valueOf(payload.get("mention_id"))),
                            Long.parseLong(String.valueOf(payload.get("cid"))),
                            Long.parseLong(String.valueOf(payload.get("mid"))),
                            Long.parseLong(String.valueOf(payload.get("from_uid"))),
                            "user",
                            Long.parseLong(String.valueOf(payload.get("uid"))),
                            BASE_TIME,
                            false
                    ));
                }
                default -> { }
            }
        }

        private ChannelMessage message(long messageId, long channelId, String status) {
            return new ChannelMessage(
                    messageId, 1001L, channelId, "Core:Text", "1.0.0", Map.of(), BASE_TIME,
                    List.of(), "", team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT))
            );
        }

        private ChannelPinReference pin(Map<String, Object> payload) {
            return new ChannelPinReference(
                    Long.parseLong(String.valueOf(payload.get("pin_id"))),
                    Long.parseLong(String.valueOf(payload.get("cid"))),
                    Long.parseLong(String.valueOf(payload.get("mid"))),
                    1001L,
                    "",
                    BASE_TIME
            );
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

package team.carrypigeon.backend.chat.domain.features.message.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.file.support.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.features.message.application.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageSenderSnapshot;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.application.server.ServerIdentityProvider;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * message 命令侧应用服务共享支撑。
 * 职责：收敛发送与治理服务共用的仓储访问、结果投影、事件发布和基础校验逻辑。
 * 边界：这里只提供内部复用能力，不作为 feature 对外暴露的稳定应用入口。
 */
abstract class AbstractMessageApplicationSupport {

    protected static final String SYSTEM_MESSAGE_TYPE = "system";
    protected static final String FILE_MESSAGE_TYPE = "file";
    protected static final String VOICE_MESSAGE_TYPE = "voice";
    protected static final String TEXT_MESSAGE_TYPE = "text";
    protected static final String CORE_TEXT_DOMAIN = "Core:Text";
    protected static final String CORE_TEXT_DOMAIN_VERSION = "1.0.0";
    protected static final String RECALLED_STATUS = "recalled";
    protected static final String MESSAGE_RECALLED_AUDIT_ACTION = "MESSAGE_RECALLED";
    protected static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    protected static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    protected static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    protected static final long MAX_PINS_PER_CHANNEL = 50L;
    protected static final long MESSAGE_EDIT_WINDOW_SECONDS = 300L;
    protected static final String RECALLED_MESSAGE_PLACEHOLDER = "[消息已撤回]";

    protected final ChannelRepository channelRepository;
    protected final ChannelMemberRepository channelMemberRepository;
    protected final ChannelAuditLogRepository channelAuditLogRepository;
    protected final ChannelPinRepository channelPinRepository;
    protected final ChannelGovernancePolicy channelGovernancePolicy;
    protected final MessageRepository messageRepository;
    protected final MentionRepository mentionRepository;
    protected final UserProfileRepository userProfileRepository;
    protected final MessageRealtimePublisher messageRealtimePublisher;
    protected final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    protected final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;
    protected final MessageAttachmentPayloadResolver messageAttachmentPayloadResolver;
    protected final ServerIdentityProvider serverIdentityProvider;
    protected final IdGenerator idGenerator;
    protected final JsonProvider jsonProvider;
    protected final TimeProvider timeProvider;
    protected final TransactionRunner transactionRunner;
    protected final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    protected AbstractMessageApplicationSupport(
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
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelPinRepository = channelPinRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.userProfileRepository = userProfileRepository;
        this.messageRealtimePublisher = messageRealtimePublisher;
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.serverIdentityProvider = serverIdentityProvider;
        this.idGenerator = idGenerator;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    protected void publishMessageCreatedAfterCommit(AfterCommitExecutor afterCommit, PersistedMessage persistedMessage) {
        afterCommit.execute(() -> {
            messageRealtimePublisher.publish(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    protected void publishMessageUpdatedAfterCommit(AfterCommitExecutor afterCommit, PersistedMessage persistedMessage) {
        afterCommit.execute(() -> {
            messageRealtimePublisher.publishUpdate(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    protected void publishMessagePinnedAfterCommit(AfterCommitExecutor afterCommit, PinnedChannelMessage pinnedChannelMessage) {
        afterCommit.execute(() -> messageRealtimePublisher.publishPin(
                pinnedChannelMessage.pin(),
                pinnedChannelMessage.recipientAccountIds()
        ));
    }

    protected void publishMessageUnpinnedAfterCommit(AfterCommitExecutor afterCommit, UnpinnedChannelMessage unpinnedChannelMessage) {
        afterCommit.execute(() -> messageRealtimePublisher.publishUnpin(
                unpinnedChannelMessage.pin(),
                unpinnedChannelMessage.unpinnedByAccountId(),
                unpinnedChannelMessage.unpinnedAt(),
                unpinnedChannelMessage.recipientAccountIds()
        ));
    }

    protected ChannelMessage withMentions(ChannelMessage message, List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
        return new ChannelMessage(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                message.searchableText(),
                message.payload(),
                message.metadata(),
                normalizeMentions(mentions),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    protected String resolveAttachmentObjectKey(Map<String, Object> data) {
        String objectKey = optionalString(data, "object_key");
        if (objectKey != null) {
            return objectKey;
        }
        String shareKey = requiredString(data, "share_key", "share_key must not be blank");
        return FileShareKeyCodec.attachmentObjectKey(shareKey)
                .orElseThrow(() -> ProblemException.validationFailed("invalid_share_key", "share_key is invalid"));
    }

    protected String normalizeAttachmentMessageType(String messageType) {
        String normalized = messageType == null || messageType.isBlank() ? FILE_MESSAGE_TYPE : messageType.trim().toLowerCase();
        if (!FILE_MESSAGE_TYPE.equals(normalized) && !VOICE_MESSAGE_TYPE.equals(normalized)) {
            throw ProblemException.validationFailed("message_type must be file or voice");
        }
        return normalized;
    }

    protected String requiredString(Map<String, Object> data, String fieldName, String message) {
        String value = optionalString(data, fieldName);
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    protected String optionalString(Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    protected Long requiredLong(Map<String, Object> data, String fieldName, String message) {
        Long value = optionalLong(data, fieldName);
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    protected Long optionalLong(Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("validation_failed", fieldName + " must be a number");
        }
    }

    protected Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    protected ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("not_channel_member", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    protected void requireSystemChannel(Channel channel) {
        if (!SYSTEM_MESSAGE_TYPE.equals(channel.type())) {
            throw ProblemException.forbidden("system_channel_required", "system message requires system channel");
        }
    }

    protected ChannelMessageResult toResult(ChannelMessage message) {
        return new ChannelMessageResult(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                message.body(),
                message.previewText(),
                messageAttachmentPayloadResolver.resolve(message.messageType(), message.payload()),
                message.metadata(),
                message.mentions(),
                message.forwardedFrom(),
                message.status(),
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    protected ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    protected ChannelPinResult toPinResult(ChannelPin pin) {
        return new ChannelPinResult(pin.pinId(), pin.channelId(), pin.messageId(), pin.pinnedByAccountId(), pin.pinnedAt(), pin.note());
    }

    protected MessageSenderSnapshot snapshotSender(long accountId) {
        return userProfileRepository.findByAccountId(accountId)
                .map(profile -> new MessageSenderSnapshot(profile.accountId(), profile.nickname(), profile.avatarUrl()))
                .orElseGet(() -> new MessageSenderSnapshot(accountId, "", ""));
    }

    protected ChannelMessage toRecalledMessage(ChannelMessage message) {
        return new ChannelMessage(
                message.messageId(),
                message.serverId(),
                message.conversationId(),
                message.channelId(),
                message.senderId(),
                message.messageType(),
                RECALLED_MESSAGE_PLACEHOLDER,
                RECALLED_MESSAGE_PLACEHOLDER,
                "",
                null,
                null,
                null,
                message.forwardedFrom(),
                RECALLED_STATUS,
                message.createdAt(),
                message.editedAt(),
                message.editVersion()
        );
    }

    protected void appendRecallAudit(long channelId, long actorAccountId, long messageId, long senderId) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                nextMessageId(),
                channelId,
                actorAccountId,
                MESSAGE_RECALLED_AUDIT_ACTION,
                senderId,
                "{\"messageId\":" + messageId + ",\"senderAccountId\":" + senderId + "}",
                now()
        ));
    }

    protected boolean isRecalled(ChannelMessage message) {
        return RECALLED_STATUS.equals(message.status());
    }

    protected void requireEditable(ChannelMessage message, long accountId) {
        if (message.senderId() != accountId || isRecalled(message)) {
            throw ProblemException.forbidden("message_not_editable", "message is not editable");
        }
        if (message.createdAt().plusSeconds(MESSAGE_EDIT_WINDOW_SECONDS).isBefore(now())) {
            throw ProblemException.forbidden("message_edit_window_expired", "message edit window expired");
        }
    }

    protected String normalizeMentions(List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> normalizedMentions = mentions.stream()
                .map(mention -> Map.<String, Object>of(
                        "type", mention.type(),
                        "uid", Long.toString(mention.uid())
                ))
                .distinct()
                .toList();
        return jsonProvider.toJson(normalizedMentions);
    }

    protected List<Mention> persistMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        List<Mention> mentions = buildMentions(message, recipientAccountIds, previousMentionsJson);
        for (Mention mention : mentions) {
            mentionRepository.save(mention);
        }
        return mentions;
    }

    protected void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            messageRealtimePublisher.publishMentionCreated(mention, List.of(mention.targetAccountId()));
        }
    }

    protected List<Mention> buildMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        if (message.mentions() == null || message.mentions().isBlank()) {
            return List.of();
        }
        Set<String> existingMentionKeys = mentionKeys(previousMentionsJson);
        Set<Long> validRecipients = new HashSet<>(recipientAccountIds);
        List<Mention> mentions = new java.util.ArrayList<>();
        for (Map<String, Object> item : jsonProvider.fromJson(message.mentions(), new TypeReference<List<Map<String, Object>>>() {
        })) {
            String type = item.get("type") == null ? null : String.valueOf(item.get("type")).trim();
            String uid = item.get("uid") == null ? null : String.valueOf(item.get("uid")).trim();
            if (!"user".equals(type) || uid == null || uid.isBlank()) {
                continue;
            }
            long targetAccountId;
            try {
                targetAccountId = Long.parseLong(uid);
            } catch (NumberFormatException exception) {
                continue;
            }
            if (targetAccountId == message.senderId() || !validRecipients.contains(targetAccountId)) {
                continue;
            }
            String mentionKey = type + ":" + targetAccountId;
            if (existingMentionKeys.contains(mentionKey)) {
                continue;
            }
            existingMentionKeys.add(mentionKey);
            mentions.add(new Mention(
                    idGenerator.nextLongId(),
                    message.channelId(),
                    message.messageId(),
                    message.senderId(),
                    type,
                    targetAccountId,
                    now(),
                    false
            ));
        }
        return mentions;
    }

    protected Set<String> mentionKeys(String mentionsJson) {
        Set<String> keys = new HashSet<>();
        if (mentionsJson == null || mentionsJson.isBlank()) {
            return keys;
        }
        for (Map<String, Object> item : jsonProvider.fromJson(mentionsJson, new TypeReference<List<Map<String, Object>>>() {
        })) {
            Object type = item.get("type");
            Object uid = item.get("uid");
            if (type != null && uid != null) {
                keys.add(String.valueOf(type).trim() + ":" + String.valueOf(uid).trim());
            }
        }
        return keys;
    }

    protected String normalizeForwardedFrom(ChannelMessage sourceMessage) {
        return jsonProvider.toJson(Map.of(
                "mid", Long.toString(sourceMessage.messageId()),
                "cid", Long.toString(sourceMessage.channelId()),
                "uid", Long.toString(sourceMessage.senderId()),
                "preview", sourceMessage.previewText() == null ? "" : sourceMessage.previewText(),
                "send_time", sourceMessage.createdAt().toEpochMilli()
        ));
    }

    protected ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    protected String resolveContentType(String messageType, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        return VOICE_MESSAGE_TYPE.equals(messageType) ? "audio/mpeg" : "application/octet-stream";
    }

    protected long nextMessageId() {
        return idGenerator.nextLongId();
    }

    protected Instant now() {
        return timeProvider.nowInstant();
    }

    protected void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    protected record PersistedMessage(
            ChannelMessage message,
            MessageSenderSnapshot senderSnapshot,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
    }

    protected record PinnedChannelMessage(ChannelPin pin, List<Long> recipientAccountIds) {
    }

    protected record UnpinnedChannelMessage(
            ChannelPin pin,
            long unpinnedByAccountId,
            long unpinnedAt,
            List<Long> recipientAccountIds
    ) {
    }
}

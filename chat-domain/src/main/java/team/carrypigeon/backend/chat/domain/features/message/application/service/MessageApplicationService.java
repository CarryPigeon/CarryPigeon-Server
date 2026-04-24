package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelAuditLog;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UploadChannelMessageAttachmentCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageHistoryResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageSearchResult;
import team.carrypigeon.backend.chat.domain.features.message.application.query.GetChannelMessageHistoryQuery;
import team.carrypigeon.backend.chat.domain.features.message.application.query.SearchChannelMessagesQuery;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.support.attachment.MessageAttachmentObjectKeyPolicy;
import team.carrypigeon.backend.chat.domain.features.message.support.payload.MessageAttachmentPayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.config.ServerIdentityProperties;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.model.StorageObject;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息应用服务。
 * 职责：编排频道消息发送、历史查询、搜索与附件上传用例。
 * 边界：应用层只编排消息规则和稳定存储抽象，不直接依赖对象存储实现。
 */
@Service
public class MessageApplicationService {

    private static final String CHANNEL_NOT_FOUND_MESSAGE = "channel does not exist";
    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String MEMBERSHIP_REQUIRED_MESSAGE = "channel membership is required";
    private static final String RECALLED_MESSAGE_PLACEHOLDER = "[消息已撤回]";

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelAuditLogRepository channelAuditLogRepository;
    private final ChannelGovernancePolicy channelGovernancePolicy;
    private final MessageRepository messageRepository;
    private final MessageRealtimePublisher messageRealtimePublisher;
    private final ChannelMessagePluginRegistry channelMessagePluginRegistry;
    private final MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy;
    private final MessageAttachmentPayloadResolver messageAttachmentPayloadResolver;
    private final ServerIdentityProperties serverIdentityProperties;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    public MessageApplicationService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            ChannelAuditLogRepository channelAuditLogRepository,
            ChannelGovernancePolicy channelGovernancePolicy,
            MessageRepository messageRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessageAttachmentObjectKeyPolicy messageAttachmentObjectKeyPolicy,
            MessageAttachmentPayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProperties serverIdentityProperties,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider
    ) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.channelAuditLogRepository = channelAuditLogRepository;
        this.channelGovernancePolicy = channelGovernancePolicy;
        this.messageRepository = messageRepository;
        this.messageRealtimePublisher = messageRealtimePublisher;
        this.channelMessagePluginRegistry = channelMessagePluginRegistry;
        this.messageAttachmentObjectKeyPolicy = messageAttachmentObjectKeyPolicy;
        this.messageAttachmentPayloadResolver = messageAttachmentPayloadResolver;
        this.serverIdentityProperties = serverIdentityProperties;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    /**
     * 发送通用频道消息。
     *
     * @param command 发送命令
     * @return 已持久化并已用于实时分发的消息结果
     */
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        validateSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, timeProvider.nowInstant());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(command.draft().type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    idGenerator.nextLongId(),
                    serverIdentityProperties.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    timeProvider.nowInstant()
            ), command.draft());
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            return new PersistedMessage(savedMessage, recipientAccountIds);
        });
        messageRealtimePublisher.publish(persistedMessage.message(), persistedMessage.recipientAccountIds());
        return toResult(persistedMessage.message());
    }

    /**
     * 发送频道文本消息。
     *
     * @param command 发送命令
     * @return 已持久化并已用于实时分发的消息结果
     */
    public ChannelMessageResult sendChannelTextMessage(SendChannelTextMessageCommand command) {
        return sendChannelMessage(new SendChannelMessageCommand(
                command.accountId(),
                command.channelId(),
                new TextChannelMessageDraft(command.body())
        ));
    }

    /**
     * 撤回频道消息。
     *
     * @param command 撤回命令
     * @return 撤回后的稳定消息结果
     */
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(() -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            ChannelMessage existingMessage = requireMessage(command.messageId());
            if (existingMessage.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            ChannelMember senderMember = channelMemberRepository.findByChannelIdAndAccountId(channel.id(), existingMessage.senderId())
                    .orElse(null);
            channelGovernancePolicy.requireCanRecallMessage(channel, operator, existingMessage, senderMember);
            if (isRecalled(existingMessage)) {
                return new PersistedMessage(existingMessage, channelMemberRepository.findAccountIdsByChannelId(channel.id()));
            }
            ChannelMessage recalledMessage = messageRepository.update(toRecalledMessage(existingMessage));
            appendRecallAudit(channel.id(), operator.accountId(), recalledMessage.messageId(), recalledMessage.senderId());
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            return new PersistedMessage(recalledMessage, recipientAccountIds);
        });
        messageRealtimePublisher.publishUpdate(persistedMessage.message(), persistedMessage.recipientAccountIds());
        return toResult(persistedMessage.message());
    }

    /**
     * 查询频道历史消息。
     *
     * @param query 历史查询参数
     * @return 历史消息结果
     */
    public ChannelMessageHistoryResult getChannelMessageHistory(GetChannelMessageHistoryQuery query) {
        validateHistoryQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.findByChannelIdBefore(
                        channel.id(),
                        query.cursorMessageId(),
                        query.limit()
                ).stream()
                .map(this::toResult)
                .toList();
        Long nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).messageId();
        return new ChannelMessageHistoryResult(messages, nextCursor);
    }

    /**
     * 在频道内按关键字搜索消息。
     *
     * @param query 搜索参数
     * @return 搜索结果
     */
    public ChannelMessageSearchResult searchChannelMessages(SearchChannelMessagesQuery query) {
        validateSearchQuery(query);
        Channel channel = requireChannel(query.channelId());
        requireMembership(channel.id(), query.accountId());
        List<ChannelMessageResult> messages = messageRepository.searchByChannelId(
                        channel.id(),
                        query.keyword().trim(),
                        query.limit()
                ).stream()
                .map(this::toResult)
                .toList();
        return new ChannelMessageSearchResult(messages);
    }

    /**
     * 上传 file / voice 消息发送前使用的附件对象。
     *
     * @param command 上传命令
     * @return 可继续用于消息发送链路的 canonical 附件信息
     */
    public ChannelMessageAttachmentUploadResult uploadChannelMessageAttachment(UploadChannelMessageAttachmentCommand command) {
        validateUploadCommand(command);
        Channel channel = requireChannel(command.channelId());
        ChannelMember member = requireMembership(channel.id(), command.accountId());
        channelGovernancePolicy.requireCanSendMessage(channel, member, timeProvider.nowInstant());
        ObjectStorageService objectStorageService = requireObjectStorageService();
        String normalizedMessageType = command.messageType().trim().toLowerCase();
        String normalizedFilename = messageAttachmentObjectKeyPolicy.normalizeFilename(command.filename());
        String resolvedContentType = resolveContentType(normalizedMessageType, command.contentType());
        String objectKey = messageAttachmentObjectKeyPolicy.buildObjectKey(
                channel.id(),
                normalizedMessageType,
                command.accountId(),
                idGenerator.nextLongId(),
                normalizedFilename
        );
        try (InputStream content = command.content()) {
            StorageObject storageObject = objectStorageService.put(new PutObjectCommand(
                    objectKey,
                    content,
                    command.size(),
                    resolvedContentType
            ));
            return new ChannelMessageAttachmentUploadResult(
                    storageObject.objectKey(),
                    normalizedFilename,
                    storageObject.contentType(),
                    storageObject.size()
            );
        } catch (IOException exception) {
            throw ProblemException.fail("attachment_stream_close_failed", "failed to close upload content");
        }
    }

    private void validateSendCommand(SendChannelTextMessageCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (command.body() == null || command.body().isBlank()) {
            throw ProblemException.validationFailed("body must not be blank");
        }
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (command.messageId() <= 0) {
            throw ProblemException.validationFailed("messageId must be greater than 0");
        }
    }

    private void validateSendCommand(SendChannelMessageCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (command.draft() == null) {
            throw ProblemException.validationFailed("draft must not be null");
        }
        if (command.draft().type() == null || command.draft().type().isBlank()) {
            throw ProblemException.validationFailed("message type must not be blank");
        }
    }

    private void validateHistoryQuery(GetChannelMessageHistoryQuery query) {
        if (query.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (query.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (query.cursorMessageId() != null && query.cursorMessageId() <= 0) {
            throw ProblemException.validationFailed("cursorMessageId must be greater than 0");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
    }

    private void validateUploadCommand(UploadChannelMessageAttachmentCommand command) {
        if (command.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (command.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        String normalizedMessageType = command.messageType() == null ? null : command.messageType().trim().toLowerCase();
        if (!"file".equals(normalizedMessageType) && !"voice".equals(normalizedMessageType)) {
            throw ProblemException.validationFailed("messageType must be file or voice");
        }
        if (command.filename() == null || command.filename().isBlank()) {
            throw ProblemException.validationFailed("filename must not be blank");
        }
        if (command.size() <= 0) {
            throw ProblemException.validationFailed("file must not be empty");
        }
        if (command.content() == null) {
            throw ProblemException.validationFailed("file content must not be null");
        }
    }

    private void validateSearchQuery(SearchChannelMessagesQuery query) {
        if (query.accountId() <= 0) {
            throw ProblemException.validationFailed("accountId must be greater than 0");
        }
        if (query.channelId() <= 0) {
            throw ProblemException.validationFailed("channelId must be greater than 0");
        }
        if (query.keyword() == null || query.keyword().isBlank()) {
            throw ProblemException.validationFailed("keyword must not be blank");
        }
        if (query.limit() <= 0 || query.limit() > 100) {
            throw ProblemException.validationFailed("limit must be between 1 and 100");
        }
    }

    private Channel requireChannel(long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> ProblemException.notFound(CHANNEL_NOT_FOUND_MESSAGE));
    }

    private ChannelMember requireMembership(long channelId, long accountId) {
        return channelMemberRepository.findByChannelIdAndAccountId(channelId, accountId)
                .orElseThrow(() -> ProblemException.forbidden("channel_member_required", MEMBERSHIP_REQUIRED_MESSAGE));
    }

    private ChannelMessageResult toResult(ChannelMessage message) {
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
                message.status(),
                message.createdAt()
        );
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private ChannelMessage toRecalledMessage(ChannelMessage message) {
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
                "recalled",
                message.createdAt()
        );
    }

    private void appendRecallAudit(long channelId, long actorAccountId, long messageId, long senderId) {
        channelAuditLogRepository.append(new ChannelAuditLog(
                idGenerator.nextLongId(),
                channelId,
                actorAccountId,
                "MESSAGE_RECALLED",
                senderId,
                "{\"messageId\":" + messageId + ",\"senderAccountId\":" + senderId + "}",
                timeProvider.nowInstant()
        ));
    }

    private boolean isRecalled(ChannelMessage message) {
        return "recalled".equals(message.status());
    }

    private ObjectStorageService requireObjectStorageService() {
        ObjectStorageService objectStorageService = objectStorageServiceProvider.getIfAvailable();
        if (objectStorageService == null) {
            throw ProblemException.fail("storage_service_unavailable", "storage service is unavailable");
        }
        return objectStorageService;
    }

    private String resolveContentType(String messageType, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.trim();
        }
        return "voice".equals(messageType) ? "audio/mpeg" : "application/octet-stream";
    }

    private record PersistedMessage(ChannelMessage message, List<Long> recipientAccountIds) {
    }
}

package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageHttpCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelTextMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendSystemChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MessageAttachmentUploadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.MessageRealtimePublisher;
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
import team.carrypigeon.backend.infrastructure.service.storage.api.model.PutObjectCommand;
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息发送应用服务。
 * 职责：承接消息发送、HTTP 消息投递、system 消息发送和附件上传等写侧能力。
 * 边界：只负责发送链路，不承接编辑、撤回、置顶等治理动作。
 */
@Service
public class MessageDeliveryApplicationService extends AbstractMessageApplicationSupport {

    public MessageDeliveryApplicationService(
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
        super(
                channelRepository,
                channelMemberRepository,
                channelAuditLogRepository,
                channelPinRepository,
                channelGovernancePolicy,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentObjectKeyPolicy,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner,
                objectStorageServiceProvider
        );
    }

    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        validateSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(command.draft().type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), command.draft());
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    public ChannelMessageResult sendChannelTextMessage(SendChannelTextMessageCommand command) {
        validateSendCommand(command);
        return sendChannelMessage(new SendChannelMessageCommand(
                command.accountId(),
                command.channelId(),
                new TextChannelMessageDraft(command.body())
        ));
    }

    public ChannelMessageResult sendChannelMessageHttp(SendChannelMessageHttpCommand command) {
        if (!CORE_TEXT_DOMAIN_VERSION.equals(command.domainVersion())) {
            throw ProblemException.validationFailed("schema_invalid", "domain version is not supported");
        }
        return switch (command.domain()) {
            case CORE_TEXT_DOMAIN -> sendHttpTextMessage(command);
            case "Core:File" -> sendHttpFileMessage(command);
            case "Core:Voice" -> sendHttpVoiceMessage(command);
            default -> throw ProblemException.validationFailed("schema_invalid", "domain is not supported");
        };
    }

    public MessageAttachmentUploadResult uploadMessageAttachment(
            long accountId,
            long channelId,
            String messageType,
            String filename,
            String contentType,
            long size,
            InputStream content
    ) {
        requirePositive(accountId, "accountId");
        requirePositive(channelId, "channelId");
        if (content == null) {
            throw ProblemException.validationFailed("file content must not be null");
        }
        if (size <= 0L) {
            throw ProblemException.validationFailed("size must be greater than 0");
        }
        String normalizedMessageType = normalizeAttachmentMessageType(messageType);
        Channel channel = requireChannel(channelId);
        ChannelMember member = requireMembership(channel.id(), accountId);
        channelGovernancePolicy.requireCanSendMessage(channel, member, now());
        String normalizedFilename = messageAttachmentObjectKeyPolicy.normalizeFilename(filename);
        String resolvedContentType = resolveContentType(normalizedMessageType, contentType);
        String objectKey = messageAttachmentObjectKeyPolicy.buildObjectKey(
                channel.id(),
                normalizedMessageType,
                accountId,
                idGenerator.nextLongId(),
                normalizedFilename
        );
        requireObjectStorageService().put(new PutObjectCommand(objectKey, content, size, resolvedContentType));
        return new MessageAttachmentUploadResult(
                objectKey,
                team.carrypigeon.backend.chat.domain.features.file.support.FileShareKeyCodec.shareKeyForObjectKey(objectKey),
                normalizedFilename,
                resolvedContentType,
                size
        );
    }

    public ChannelMessageResult sendSystemChannelMessage(SendSystemChannelMessageCommand command) {
        validateSystemSendCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            requireSystemChannel(channel);
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(SYSTEM_MESSAGE_TYPE);
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.operatorAccountId(),
                    now()
            ), new SystemChannelMessageDraft(command.body(), command.payload(), command.metadata()));
            ChannelMessage savedMessage = messageRepository.save(message);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    private void validateSendCommand(SendChannelTextMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.body() == null || command.body().isBlank()) {
            throw ProblemException.validationFailed("body must not be blank");
        }
    }

    private void validateSystemSendCommand(SendSystemChannelMessageCommand command) {
        requirePositive(command.operatorAccountId(), "operatorAccountId");
        requirePositive(command.channelId(), "channelId");
    }

    private void validateSendCommand(SendChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        if (command.draft() == null) {
            throw ProblemException.validationFailed("draft must not be null");
        }
        if (command.draft().type() == null || command.draft().type().isBlank()) {
            throw ProblemException.validationFailed("message type must not be blank");
        }
    }

    private ChannelMessageResult sendHttpTextMessage(SendChannelMessageHttpCommand command) {
        String normalizedText = requiredString(command.data(), "text", "text must not be blank");
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    TEXT_MESSAGE_TYPE,
                    normalizedText,
                    normalizedText,
                    normalizedText,
                    null,
                    null,
                    normalizeMentions(command.mentions()),
                    null,
                    "sent",
                    now(),
                    null,
                    1L
            ));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }

    private ChannelMessageResult sendHttpFileMessage(SendChannelMessageHttpCommand command) {
        String filename = requiredString(command.data(), "filename", "filename must not be blank");
        String body = optionalString(command.data(), "text");
        Long size = optionalLong(command.data(), "size");
        String mimeType = optionalString(command.data(), "mime_type");
        return sendAttachmentHttpMessage(command, new FileChannelMessageDraft(
                body,
                resolveAttachmentObjectKey(command.data()),
                filename,
                mimeType,
                size,
                null
        ));
    }

    private ChannelMessageResult sendHttpVoiceMessage(SendChannelMessageHttpCommand command) {
        String filename = requiredString(command.data(), "filename", "filename must not be blank");
        Long durationMillis = requiredLong(command.data(), "duration_millis", "duration_millis must be greater than 0");
        String body = optionalString(command.data(), "text");
        Long size = optionalLong(command.data(), "size");
        String mimeType = optionalString(command.data(), "mime_type");
        String transcript = optionalString(command.data(), "transcript");
        return sendAttachmentHttpMessage(command, new VoiceChannelMessageDraft(
                body,
                resolveAttachmentObjectKey(command.data()),
                filename,
                mimeType,
                size,
                durationMillis,
                transcript,
                null
        ));
    }

    private ChannelMessageResult sendAttachmentHttpMessage(
            SendChannelMessageHttpCommand command,
            ChannelMessageDraft draft
    ) {
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember member = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanSendMessage(channel, member, now());
            ChannelMessagePlugin plugin = channelMessagePluginRegistry.require(draft.type());
            ChannelMessage message = plugin.createMessage(new ChannelMessagePlugin.ChannelMessageBuildContext(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    channel.conversationId(),
                    channel.id(),
                    command.accountId(),
                    now()
            ), draft);
            ChannelMessage savedMessage = messageRepository.save(withMentions(message, command.mentions()));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, null);
            PersistedMessage createdMessage = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageCreatedAfterCommit(afterCommit, createdMessage);
            return createdMessage;
        });
        return toResult(persistedMessage.message());
    }
}

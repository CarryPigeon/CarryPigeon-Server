package team.carrypigeon.backend.chat.domain.features.message.application.service;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.Channel;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelMember;
import team.carrypigeon.backend.chat.domain.features.channel.domain.model.ChannelPin;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelAuditLogRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelMemberRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelPinRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.repository.ChannelRepository;
import team.carrypigeon.backend.chat.domain.features.channel.domain.service.ChannelGovernancePolicy;
import team.carrypigeon.backend.chat.domain.features.message.application.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.ForwardChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.PinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.command.UnpinChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.ChannelPinResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
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
import team.carrypigeon.backend.infrastructure.service.storage.api.service.ObjectStorageService;

/**
 * 消息治理应用服务。
 * 职责：承接转发、编辑、撤回、删除、置顶和取消置顶等治理型写侧能力。
 * 边界：不负责消息发送与附件写入。
 */
@Service
public class MessageModerationApplicationService extends AbstractMessageApplicationSupport {

    public MessageModerationApplicationService(
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

    public ChannelMessageResult forwardChannelMessage(ForwardChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.sourceMessageId(), "sourceMessageId");
        requirePositive(command.targetChannelId(), "targetChannelId");
        if (command.comment() != null && command.comment().trim().length() > 500) {
            throw ProblemException.validationFailed("comment length must be less than or equal to 500");
        }
        ChannelMessage sourceMessage = requireMessage(command.sourceMessageId());
        Channel targetChannel = requireChannel(command.targetChannelId());
        requireMembership(targetChannel.id(), command.accountId());
        StringBuilder forwardedText = new StringBuilder();
        if (command.comment() != null && !command.comment().isBlank()) {
            forwardedText.append(command.comment().trim()).append("\n\n");
        }
        forwardedText.append("[Forwarded] ").append(sourceMessage.previewText() == null ? "" : sourceMessage.previewText());
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage savedMessage = messageRepository.save(new ChannelMessage(
                    nextMessageId(),
                    serverIdentityProvider.id(),
                    targetChannel.conversationId(),
                    targetChannel.id(),
                    command.accountId(),
                    TEXT_MESSAGE_TYPE,
                    forwardedText.toString(),
                    forwardedText.toString(),
                    forwardedText.toString(),
                    null,
                    null,
                    null,
                    normalizeForwardedFrom(sourceMessage),
                    "sent",
                    now(),
                    null,
                    1L
            ));
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(targetChannel.id());
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

    public ChannelMessageResult editChannelMessage(EditChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.messageId(), "messageId");
        if (!CORE_TEXT_DOMAIN.equals(command.domain())) {
            throw ProblemException.validationFailed("schema_invalid", "domain is not supported");
        }
        if (!CORE_TEXT_DOMAIN_VERSION.equals(command.domainVersion())) {
            throw ProblemException.validationFailed("schema_invalid", "domain version is not supported");
        }
        if (command.text() == null || command.text().isBlank()) {
            throw ProblemException.validationFailed("validation_failed", "text must not be blank");
        }
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            Channel channel = requireChannel(existingMessage.channelId());
            requireMembership(channel.id(), command.accountId());
            requireEditable(existingMessage, command.accountId());
            if (command.expectedEditVersion() != null && command.expectedEditVersion() != existingMessage.editVersion()) {
                throw ProblemException.conflict("conflict", "message edit version conflict");
            }
            ChannelMessage updatedMessage = new ChannelMessage(
                    existingMessage.messageId(),
                    existingMessage.serverId(),
                    existingMessage.conversationId(),
                    existingMessage.channelId(),
                    existingMessage.senderId(),
                    existingMessage.messageType(),
                    command.text().trim(),
                    command.text().trim(),
                    command.text().trim(),
                    existingMessage.payload(),
                    existingMessage.metadata(),
                    normalizeMentions(command.mentions()),
                    existingMessage.forwardedFrom(),
                    existingMessage.status(),
                    existingMessage.createdAt(),
                    now(),
                    existingMessage.editVersion() + 1
            );
            ChannelMessage savedMessage = messageRepository.update(updatedMessage);
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            List<Mention> mentions = persistMentions(savedMessage, recipientAccountIds, existingMessage.mentions());
            PersistedMessage updatedResult = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            publishMessageUpdatedAfterCommit(afterCommit, updatedResult);
            return updatedResult;
        });
        return toResult(persistedMessage.message());
    }

    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
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
                PersistedMessage recalledResult = new PersistedMessage(
                        existingMessage,
                        snapshotSender(existingMessage.senderId()),
                        channelMemberRepository.findAccountIdsByChannelId(channel.id()),
                        List.of()
                );
                publishMessageUpdatedAfterCommit(afterCommit, recalledResult);
                return recalledResult;
            }
            ChannelMessage recalledMessage = messageRepository.update(toRecalledMessage(existingMessage));
            appendRecallAudit(channel.id(), operator.accountId(), recalledMessage.messageId(), recalledMessage.senderId());
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage recalledResult = new PersistedMessage(
                    recalledMessage,
                    snapshotSender(recalledMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageUpdatedAfterCommit(afterCommit, recalledResult);
            return recalledResult;
        });
        return toResult(persistedMessage.message());
    }

    public void deleteChannelMessage(DeleteChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            Channel channel = requireChannel(existingMessage.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            ChannelMember senderMember = channelMemberRepository.findByChannelIdAndAccountId(channel.id(), existingMessage.senderId()).orElse(null);
            channelGovernancePolicy.requireCanRecallMessage(channel, operator, existingMessage, senderMember);
            messageRepository.delete(existingMessage.messageId());
            List<Long> recipientAccountIds = channelMemberRepository.findAccountIdsByChannelId(channel.id());
            PersistedMessage deletedMessage = new PersistedMessage(
                    toRecalledMessage(existingMessage),
                    snapshotSender(existingMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            publishMessageUpdatedAfterCommit(afterCommit, deletedMessage);
        });
    }

    public ChannelPinResult pinChannelMessage(PinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        if (command.note() != null && command.note().trim().length() > 200) {
            throw ProblemException.validationFailed("note length must be less than or equal to 200");
        }
        PinnedChannelMessage pinnedChannelMessage = transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanModeratePin(channel, operator);
            ChannelMessage message = requireMessage(command.messageId());
            if (message.channelId() != channel.id()) {
                throw ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE);
            }
            if (channelPinRepository.findByChannelIdAndMessageId(channel.id(), message.messageId()).isEmpty()
                    && channelPinRepository.countByChannelId(channel.id()) >= MAX_PINS_PER_CHANNEL) {
                throw ProblemException.validationFailed("pin_limit_reached", "channel pin limit is reached");
            }
            ChannelPin pin = new ChannelPin(
                    idGenerator.nextLongId(),
                    channel.id(),
                    message.messageId(),
                    operator.accountId(),
                    command.note() == null ? "" : command.note().trim(),
                    now()
            );
            channelPinRepository.save(pin);
            PinnedChannelMessage pinnedResult = new PinnedChannelMessage(pin, channelMemberRepository.findAccountIdsByChannelId(channel.id()));
            publishMessagePinnedAfterCommit(afterCommit, pinnedResult);
            return pinnedResult;
        });
        return toPinResult(pinnedChannelMessage.pin());
    }

    public void unpinChannelMessage(UnpinChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            Channel channel = requireChannel(command.channelId());
            ChannelMember operator = requireMembership(channel.id(), command.accountId());
            channelGovernancePolicy.requireCanModeratePin(channel, operator);
            ChannelPin pin = channelPinRepository.findByChannelIdAndMessageId(channel.id(), command.messageId())
                    .orElseThrow(() -> ProblemException.notFound("channel pin does not exist"));
            channelPinRepository.delete(channel.id(), command.messageId());
            UnpinnedChannelMessage unpinnedChannelMessage = new UnpinnedChannelMessage(
                    pin,
                    operator.accountId(),
                    now().toEpochMilli(),
                    channelMemberRepository.findAccountIdsByChannelId(channel.id())
            );
            publishMessageUnpinnedAfterCommit(afterCommit, unpinnedChannelMessage);
        });
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }
}

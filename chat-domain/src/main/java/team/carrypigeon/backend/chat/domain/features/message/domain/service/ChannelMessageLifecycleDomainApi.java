package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.DeleteChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageChannelBoundary;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessagePayloadResolver;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.user.domain.repository.UserProfileRepository;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.chat.domain.shared.domain.server.ServerIdentityProvider;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息生命周期领域 API 实现。
 * 职责：直接承载消息编辑、撤回和删除用例。
 * 边界：不承载消息发布、查询、附件上传和置顶能力。
 */
@Service
public class ChannelMessageLifecycleDomainApi extends AbstractMessageDomainSupport implements ChannelMessageLifecycleApi {

    public ChannelMessageLifecycleDomainApi(
            MessageChannelBoundary messageChannelBoundary,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            UserProfileRepository userProfileRepository,
            MessageRealtimePublisher messageRealtimePublisher,
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            MessagePayloadResolver messageAttachmentPayloadResolver,
            ServerIdentityProvider serverIdentityProvider,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        super(
                messageChannelBoundary,
                messageRepository,
                mentionRepository,
                userProfileRepository,
                messageRealtimePublisher,
                channelMessagePluginRegistry,
                messageAttachmentPayloadResolver,
                serverIdentityProvider,
                idGenerator,
                jsonProvider,
                timeProvider,
                transactionRunner
        );
    }

    @Override
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
            MessageChannelBoundary.MessageChannel channel = requireMemberChannel(existingMessage.channelId(), command.accountId());
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
                    messageMentionManager.normalizeMentions(command.mentions()),
                    existingMessage.forwardedFrom(),
                    existingMessage.status(),
                    existingMessage.createdAt(),
                    now(),
                    existingMessage.editVersion() + 1
            );
            ChannelMessage savedMessage = messageRepository.update(updatedMessage);
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            List<Mention> mentions = messageMentionManager.persistMentions(savedMessage, recipientAccountIds, existingMessage.mentions());
            PersistedMessage updatedResult = new PersistedMessage(
                    savedMessage,
                    snapshotSender(savedMessage.senderId()),
                    recipientAccountIds,
                    mentions
            );
            messageAfterCommitPublisher.publishMessageUpdatedAfterCommit(afterCommit, updatedResult);
            return updatedResult;
        });
        return toResult(persistedMessage.message());
    }

    @Override
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            MessageChannelBoundary.MessageChannel channel = requireChannel(command.channelId());
            ChannelMessage existingMessage = requireMessage(command.messageId());
            messageChannelBoundary.requireRecallPermission(channel.id(), command.accountId(), existingMessage);
            if (isRecalled(existingMessage)) {
                return new PersistedMessage(
                        existingMessage,
                        snapshotSender(existingMessage.senderId()),
                        messageChannelBoundary.recipientAccountIds(channel.id()),
                        List.of()
                );
            }
            ChannelMessage recalledMessage = messageRepository.update(toRecalledMessage(existingMessage));
            appendRecallAudit(channel.id(), command.accountId(), recalledMessage.messageId(), recalledMessage.senderId());
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            PersistedMessage recalledResult = new PersistedMessage(
                    recalledMessage,
                    snapshotSender(recalledMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            messageAfterCommitPublisher.publishMessageUpdatedAfterCommit(afterCommit, recalledResult);
            return recalledResult;
        });
        return toResult(persistedMessage.message());
    }

    @Override
    public void deleteChannelMessage(DeleteChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.messageId(), "messageId");
        transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            MessageChannelBoundary.MessageChannel channel = requireChannel(existingMessage.channelId());
            messageChannelBoundary.requireRecallPermission(channel.id(), command.accountId(), existingMessage);
            messageChannelBoundary.deletePinsByMessageId(existingMessage.messageId());
            messageMentionManager.deleteByMessageId(existingMessage.messageId());
            messageRepository.delete(existingMessage.messageId());
            List<Long> recipientAccountIds = messageChannelBoundary.recipientAccountIds(channel.id());
            PersistedMessage deletedMessage = new PersistedMessage(
                    toRecalledMessage(existingMessage),
                    snapshotSender(existingMessage.senderId()),
                    recipientAccountIds,
                    List.of()
            );
            messageAfterCommitPublisher.publishMessageDeletedAfterCommit(afterCommit, deletedMessage);
        });
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }
}

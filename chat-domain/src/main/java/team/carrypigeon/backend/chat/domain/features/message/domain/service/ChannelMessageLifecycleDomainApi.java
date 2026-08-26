package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessageAuditApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagePolicyApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.AppendMessageRecallAuditCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息生命周期领域 API 实现。
 * 职责：执行 sent 到 recalled 的唯一状态转换。
 * 边界：不提供编辑或硬删除能力。
 */
@Service
public class ChannelMessageLifecycleDomainApi implements ChannelMessageLifecycleApi {

    private static final String MESSAGE_NOT_FOUND_MESSAGE = "message does not exist";
    private static final String RECALLED_MESSAGE_PLACEHOLDER = "消息已撤回";

    private final ChannelContextApi channelContextApi;
    private final ChannelMessagePolicyApi channelMessagePolicyApi;
    private final ChannelMessageAuditApi channelMessageAuditApi;
    private final MessageRepository messageRepository;
    private final MessageMentionManager messageMentionManager;
    private final MessageAfterCommitPublisher messageAfterCommitPublisher;
    private final IdGenerator idGenerator;
    private final TimeProvider timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelMessageLifecycleDomainApi(
            ChannelContextApi channelContextApi,
            ChannelMessagePolicyApi channelMessagePolicyApi,
            ChannelMessageAuditApi channelMessageAuditApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            ApplicationEventPublisher eventPublisher,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelContextApi = channelContextApi;
        this.channelMessagePolicyApi = channelMessagePolicyApi;
        this.channelMessageAuditApi = channelMessageAuditApi;
        this.messageRepository = messageRepository;
        this.messageMentionManager = new MessageMentionManager(mentionRepository, idGenerator, timeProvider);
        this.messageAfterCommitPublisher = new MessageAfterCommitPublisher(eventPublisher, timeProvider);
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        ChannelMessage recalledMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = channelContextApi.requireChannel(command.channelId());
            ChannelMessage existingMessage = requireMessage(command.messageId());
            channelMessagePolicyApi.requireRecallPermission(new RequireMessageRecallPermissionCommand(
                    channel.id(),
                    command.accountId(),
                    existingMessage.channelId(),
                    existingMessage.senderId()
            ));
            List<Long> recipients = channelContextApi.recipientAccountIds(channel.id());
            if (isRecalled(existingMessage)) {
                return existingMessage;
            }
            ChannelMessage updatedMessage = messageRepository.update(toRecalledMessage(existingMessage));
            messageMentionManager.deleteByMessageId(updatedMessage.messageId());
            channelMessageAuditApi.appendMessageRecallAudit(new AppendMessageRecallAuditCommand(
                    nextMessageId(), channel.id(), command.accountId(), updatedMessage.messageId(),
                    updatedMessage.senderId(), now()
            ));
            messageAfterCommitPublisher.publishMessageRecalledAfterCommit(afterCommit, updatedMessage, recipients);
            return updatedMessage;
        });
        return ChannelMessageProjectionMapper.toResult(recalledMessage);
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.notFound(MESSAGE_NOT_FOUND_MESSAGE));
    }

    private ChannelMessage toRecalledMessage(ChannelMessage message) {
        return new ChannelMessage(
                message.messageId(),
                message.senderId(),
                message.channelId(),
                message.domain(),
                message.domainVersion(),
                Map.of(),
                message.sendTime(),
                List.of(),
                RECALLED_MESSAGE_PLACEHOLDER,
                MessageStatus.RECALLED
        );
    }

    private boolean isRecalled(ChannelMessage message) {
        return message.status() == MessageStatus.RECALLED;
    }

    private long nextMessageId() {
        return idGenerator.nextLongId();
    }

    private java.time.Instant now() {
        return timeProvider.nowInstant();
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }
}

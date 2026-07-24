package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelMessagingApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.command.RequireMessageRecallPermissionCommand;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelMessagingContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 频道消息生命周期领域 API 实现。
 * 职责：执行 sent 到 recalled 的唯一状态转换。
 * 边界：不提供编辑或硬删除能力。
 */
@Service
public class ChannelMessageLifecycleDomainApi extends AbstractMessageDomainSupport implements ChannelMessageLifecycleApi {

    public ChannelMessageLifecycleDomainApi(
            ChannelMessagingApi channelMessagingApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            RealtimeEventApi realtimeEventApi,
            IdGenerator idGenerator,
            TimeProvider timeProvider,
            TransactionRunner transactionRunner
    ) {
        super(
                channelMessagingApi,
                messageRepository,
                mentionRepository,
                realtimeEventApi,
                idGenerator,
                timeProvider,
                transactionRunner
        );
    }

    @Override
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        PersistedMessage persistedMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessagingContext channel = requireChannel(command.channelId());
            ChannelMessage existingMessage = requireMessage(command.messageId());
            channelMessagingApi.requireRecallPermission(new RequireMessageRecallPermissionCommand(
                    channel.id(),
                    command.accountId(),
                    existingMessage.channelId(),
                    existingMessage.senderId()
            ));
            List<Long> recipients = channelMessagingApi.recipientAccountIds(channel.id());
            if (isRecalled(existingMessage)) {
                return new PersistedMessage(existingMessage, recipients, List.of());
            }
            ChannelMessage recalledMessage = messageRepository.update(toRecalledMessage(existingMessage));
            messageMentionManager.deleteByMessageId(recalledMessage.messageId());
            appendRecallAudit(channel.id(), command.accountId(), recalledMessage.messageId(), recalledMessage.senderId());
            PersistedMessage result = new PersistedMessage(recalledMessage, recipients, List.of());
            messageAfterCommitPublisher.publishMessageRecalledAfterCommit(afterCommit, result);
            return result;
        });
        return toResult(persistedMessage.message());
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }
}

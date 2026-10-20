package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageLifecycleApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.MessageStatus;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 频道消息生命周期领域 API 实现。
 * 职责：执行 sent 到 recalled 的唯一状态转换。
 * 边界：不提供编辑或硬删除能力。
 */
@Service
public class ChannelMessageLifecycleDomainApi implements ChannelMessageLifecycleApi {

    private static final String RECALLED_MESSAGE_PLACEHOLDER = "消息已撤回";

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final MessageEventNotifier messageEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;

    public ChannelMessageLifecycleDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageEventNotifier messageEventNotifier,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.messageEventNotifier = messageEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        ChannelMessage recalledMessage = transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage existingMessage = requireMessage(command.messageId());
            // 以消息自身的频道为准，避免命令频道与消息频道不一致时越权
            ChannelContext operatorContext = channelContextApi.requireMemberContext(
                existingMessage.channelId(), command.accountId());
            // 发送者也必须仍是频道成员，避免对已退出成员的历史消息执行治理
            channelContextApi.requireMemberContext(existingMessage.channelId(), existingMessage.senderId());
            requireRecallPermitted(existingMessage, command.accountId(), operatorContext);
            List<Long> recipients = channelContextApi.memberAccountIds(existingMessage.channelId());
            if (isRecalled(existingMessage)) {
                return existingMessage;
            }
            ChannelMessage updatedMessage = messageRepository.update(toRecalledMessage(existingMessage));
            mentionRepository.deleteByMessageId(updatedMessage.messageId());
            messageEventNotifier.messageRecalled(afterCommit, updatedMessage, recipients);
            return updatedMessage;
        });
        return ChannelMessageResult.from(recalledMessage);
    }

    /**
     * 校验调用方是否有权撤回目标消息。
     * 约束：本人可撤回自己的消息；治理者可撤回非治理者成员的消息，治理者之间不可互相撤回。
     *
     * @param message 待撤回消息
     * @param operatorAccountId 撤回操作者账号 ID
     * @param operatorContext 操作者在消息所属频道的上下文
     */
    private void requireRecallPermitted(ChannelMessage message, long operatorAccountId, ChannelContext operatorContext) {
        if (message.senderId() == operatorAccountId) {
            return;
        }
        if (operatorContext.isModerator()
                && !channelContextApi.moderatorAccounts(message.channelId()).contains(message.senderId())) {
            return;
        }
        throw ProblemException.of(MessageProblem.RECALL_FORBIDDEN);
    }

    private void validateRecallCommand(RecallChannelMessageCommand command) {
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.messageId(), "messageId");
    }

    private ChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
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

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, fieldName + " must be greater than 0");
        }
    }
}

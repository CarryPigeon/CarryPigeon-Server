package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageIdempotencyRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.message.MessageDomainPluginApi;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageDispatcher;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPMessageStatus;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

import java.util.List;
import java.util.Map;

/**
 * 频道消息发布领域 API 实现。
 * 职责：创建 canonical 消息，并实现 ReplyText 与单条/合并 Forward。
 * 边界：不承载撤回、查询、附件上传和置顶能力。
 */
@Service
public class ChannelMessageDomainApi implements ChannelMessageApi {

    private static final String RECALLED_MESSAGE_PLACEHOLDER = "消息已撤回";
    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final MessageEventNotifier messageEventNotifier;
    private final MessageIdempotencyRepository messageIdempotencyRepository;
    private final MessageDomainPluginApi messageDomainPluginApi;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;
    private final CPChannelMessageDispatcher cpChannelMessageDispatcher;

    public ChannelMessageDomainApi(
        ChannelContextApi channelContextApi,
        MessageRepository messageRepository,
        MentionRepository mentionRepository,
        MessageIdempotencyRepository messageIdempotencyRepository,
        MessageEventNotifier messageEventNotifier,
        MessageDomainPluginApi messageDomainPluginApi,
        IdGenerator idGenerator,
        TimeProviderImpl timeProvider,
        TransactionRunner transactionRunner, CPChannelMessageDispatcher cpChannelMessageDispatcher
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.messageEventNotifier = messageEventNotifier;
        this.messageIdempotencyRepository = messageIdempotencyRepository;
        this.messageDomainPluginApi = messageDomainPluginApi;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.cpChannelMessageDispatcher = cpChannelMessageDispatcher;
    }

    @Override
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        CPChannelMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            // 权限校验
            ChannelContext channelContext = channelContextApi.requireMemberContext(command.channelId(), command.accountId());
            // 判断是否能够发送消息
            if (channelContext.isMuted()){
                throw ProblemException.of(MessageProblem.USER_MUTED);
            }
            // 组装消息
            CPChannelMessage message = new CPChannelMessage(
                idGenerator.nextLongId(),
                command.accountId(),
                command.channelId(),
                command.domain(),
                command.data(),
                timeProvider.nowInstant(),
                command.mentions(),
                "",
                CPMessageStatus.SENT
            );
            // 获取插件处理器
            CPChannelMessageHandler handler = cpChannelMessageDispatcher.dispatch(command.domain());
            // 调用消息插件对消息进行解析成入库的统一形式
            CPChannelMessage channelMessage = handler.handle(message);
            // 消息持久化
            messageRepository.save(channelMessage);
            // 通知群组所在的所有人
            messageEventNotifier.messageCreated(afterCommit, channelMessage, channelContextApi.memberAccountIds(command.channelId()));
            // 返回响应值
            return channelMessage;
        });
        return ChannelMessageResult.from(persisted);
    }

    @Override
    public ChannelMessageResult recallChannelMessage(RecallChannelMessageCommand command) {
        validateRecallCommand(command);
        CPChannelMessage recalledMessage = transactionRunner.runInTransaction(afterCommit -> {
            CPChannelMessage existingMessage = requireMessage(command.messageId());
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
            CPChannelMessage updatedMessage = messageRepository.update(toRecalledMessage(existingMessage));
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
    private void requireRecallPermitted(CPChannelMessage message, long operatorAccountId, ChannelContext operatorContext) {
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

    private CPChannelMessage requireMessage(long messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
    }

    private CPChannelMessage toRecalledMessage(CPChannelMessage message) {
        return new CPChannelMessage(
            message.messageId(),
            message.senderId(),
            message.channelId(),
            message.domain(),
            Map.of(),
            message.sendTime(),
            List.of(),
            RECALLED_MESSAGE_PLACEHOLDER,
            CPMessageStatus.RECALLED
        );
    }

    private boolean isRecalled(CPChannelMessage message) {
        return message.status() == CPMessageStatus.RECALLED;
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.of(MessageProblem.INVALID_INPUT, fieldName + " must be greater than 0");
        }
    }
}

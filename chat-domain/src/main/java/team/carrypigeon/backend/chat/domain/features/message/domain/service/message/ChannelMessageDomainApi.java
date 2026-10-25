package team.carrypigeon.backend.chat.domain.features.message.domain.service.message;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.result.ChannelContext;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.ChannelMessageApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.RecallChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.input.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.message.result.ChannelMessageResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageDispatcher;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessageHandler;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPMessageStatus;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 频道消息发布领域 API 实现。
 * 职责：编排消息发送、撤回、持久化与提交后通知。
 * 边界：不承载请求字段校验；只处理频道权限、禁言、domain 支持、消息状态和事务。
 */
@Service
public class ChannelMessageDomainApi implements ChannelMessageApi {

    private final ChannelContextApi channelContextApi;
    private final MessageRepository messageRepository;
    private final MentionRepository mentionRepository;
    private final MessageEventNotifier messageEventNotifier;
    private final IdGenerator idGenerator;
    private final TimeProviderImpl timeProvider;
    private final TransactionRunner transactionRunner;
    private final CPChannelMessageDispatcher channelMessageDispatcher;

    public ChannelMessageDomainApi(
            ChannelContextApi channelContextApi,
            MessageRepository messageRepository,
            MentionRepository mentionRepository,
            MessageEventNotifier messageEventNotifier,
            IdGenerator idGenerator,
            TimeProviderImpl timeProvider,
            TransactionRunner transactionRunner,
            CPChannelMessageDispatcher channelMessageDispatcher
    ) {
        this.channelContextApi = channelContextApi;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.messageEventNotifier = messageEventNotifier;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.transactionRunner = transactionRunner;
        this.channelMessageDispatcher = channelMessageDispatcher;
    }

    @Override
    public ChannelMessageResult sendChannelMessage(SendChannelMessageCommand command) {
        CPChannelMessage persisted = transactionRunner.runInTransaction(afterCommit -> {
            ChannelContext channelContext = channelContextApi.requireMemberContext(command.channelId(), command.accountId());
            if (channelContext.isMuted()) {
                throw ProblemException.of(MessageProblem.USER_MUTED);
            }
            if (!channelMessageDispatcher.supports(command.domain())) {
                throw ProblemException.of(MessageProblem.UNSUPPORTED_DOMAIN);
            }
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
            CPChannelMessageHandler handler = channelMessageDispatcher.dispatch(command.domain());
            CPChannelMessage saved = messageRepository.save(handler.handle(message));
            messageEventNotifier.messageCreated(
                    afterCommit,
                    saved,
                    channelContextApi.memberAccountIds(command.channelId())
            );
            return saved;
        });
        return ChannelMessageResult.from(persisted);
    }

    @Override
    public void recallChannelMessage(RecallChannelMessageCommand command) {
        transactionRunner.runInTransaction(afterCommit -> {
            CPChannelMessage existingMessage = messageRepository.findById(command.messageId())
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            if (existingMessage.channelId() != command.channelId()) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            ChannelContext operatorContext = channelContextApi.requireMemberContext(
                    existingMessage.channelId(),
                    command.accountId()
            );
            boolean senderRecallingOwnMessage = existingMessage.senderId() == command.accountId();
            boolean moderatorRecallingMemberMessage = operatorContext.isModerator()
                    && !channelContextApi.moderatorAccounts(existingMessage.channelId()).contains(existingMessage.senderId());
            if (operatorContext.isMuted() || (!senderRecallingOwnMessage && !moderatorRecallingMemberMessage)) {
                throw ProblemException.of(MessageProblem.RECALL_FORBIDDEN);
            }
            if (existingMessage.status() == CPMessageStatus.RECALLED) {
                return;
            }
            List<Long> recipients = channelContextApi.memberAccountIds(existingMessage.channelId());
            CPChannelMessage recalledMessage = new CPChannelMessage(
                    existingMessage.messageId(),
                    existingMessage.senderId(),
                    existingMessage.channelId(),
                    existingMessage.domain(),
                    Map.of(),
                    existingMessage.sendTime(),
                    List.of(),
                    "消息已撤回",
                    CPMessageStatus.RECALLED
            );
            messageRepository.update(recalledMessage);
            mentionRepository.deleteByMessageId(existingMessage.messageId());
            messageEventNotifier.messageRecalled(afterCommit, recalledMessage, recipients);
        });
    }
}

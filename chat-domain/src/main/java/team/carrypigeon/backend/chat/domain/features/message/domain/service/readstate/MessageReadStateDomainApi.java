package team.carrypigeon.backend.chat.domain.features.message.domain.service.readstate;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.input.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.IdUtil;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 消息读状态领域 API 实现。
 * 职责：校验最后已读消息的业务归属与频道成员关系，并维护只前进不后退的读状态。
 * 边界：不承载请求字段校验；未读统计由读状态仓储提供，不读取 channel 内部模型或仓储。
 */
@Service
public class MessageReadStateDomainApi implements MessageReadStateApi {

    private final MessageRepository messageRepository;
    private final ChannelReadStateRepository readStateRepository;
    private final ChannelContextApi channelContextApi;
    private final TransactionRunner transactionRunner;
    private final MessageEventNotifier messageEventNotifier;

    public MessageReadStateDomainApi(
            MessageRepository messageRepository,
            ChannelReadStateRepository readStateRepository,
            ChannelContextApi channelContextApi,
            TransactionRunner transactionRunner,
            MessageEventNotifier messageEventNotifier
    ) {
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
        this.channelContextApi = channelContextApi;
        this.transactionRunner = transactionRunner;
        this.messageEventNotifier = messageEventNotifier;
    }

    @Override
    public ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command) {
        return transactionRunner.runInTransaction(afterCommit -> {
            CPChannelMessage message = messageRepository.findById(command.lastReadMessageId())
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            if (message.channelId() != command.channelId()) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            channelContextApi.requireMemberContext(command.channelId(), command.accountId());
            ChannelReadState current = readStateRepository
                    .findByChannelIdAndAccountId(command.channelId(), command.accountId())
                    .orElse(null);
            if (current != null && current.lastReadMessageId() >= command.lastReadMessageId()) {
                return toResult(current);
            }
            Instant readTime = Instant.ofEpochMilli(command.lastReadTime());
            ChannelReadState updated = new ChannelReadState(
                    command.channelId(),
                    command.accountId(),
                    command.lastReadMessageId(),
                    readTime,
                    current == null ? readTime : current.createdAt(),
                    readTime
            );
            if (readStateRepository.advanceIfNewer(updated)) {
                messageEventNotifier.readStateUpdated(afterCommit, updated);
                return toResult(updated);
            }
            return readStateRepository.findByChannelIdAndAccountId(command.channelId(), command.accountId())
                    .map(this::toResult)
                    .orElseGet(() -> toResult(updated));
        });
    }

    @Override
    public List<ChannelUnreadResult> listUnreads(long accountId) {
        return readStateRepository.listUnreadsByAccountId(accountId).stream()
                .map(item -> new ChannelUnreadResult(
                        IdUtil.toString(item.channelId()),
                        item.unreadCount(),
                        item.lastReadTime() == null ? 0L : item.lastReadTime().toEpochMilli()
                ))
                .toList();
    }

    private ChannelReadStateResult toResult(ChannelReadState readState) {
        return new ChannelReadStateResult(
                IdUtil.toString(readState.channelId()),
                IdUtil.toString(readState.accountId()),
                IdUtil.toString(readState.lastReadMessageId()),
                readState.lastReadTime().toEpochMilli()
        );
    }
}

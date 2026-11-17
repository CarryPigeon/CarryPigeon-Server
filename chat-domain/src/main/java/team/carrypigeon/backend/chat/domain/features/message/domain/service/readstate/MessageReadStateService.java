package team.carrypigeon.backend.chat.domain.features.message.domain.service.readstate;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.problem.MessageProblem;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.api.CPChannelMessage;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner;

/**
 * 消息读状态领域 API 实现。
 * 职责：校验最后已读消息的业务归属与频道成员关系，并维护只前进不后退的读状态。
 * 边界：不承载请求字段校验；未读统计由读状态仓储提供，不读取 channel 内部模型或仓储。
 */
@Service
public class MessageReadStateService implements MessageReadStateApi {

    private final MessageRepository messageRepository;
    private final ChannelReadStateRepository readStateRepository;
    private final ChannelContextApi channelContextApi;
    private final TransactionRunner transactionRunner;
    private final MessageEventNotifier messageEventNotifier;
    private final TimeProvider timeProvider;

    public MessageReadStateService(
            MessageRepository messageRepository,
            ChannelReadStateRepository readStateRepository,
            ChannelContextApi channelContextApi,
            TransactionRunner transactionRunner,
            MessageEventNotifier messageEventNotifier,
            TimeProvider timeProvider
    ) {
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
        this.channelContextApi = channelContextApi;
        this.transactionRunner = transactionRunner;
        this.messageEventNotifier = messageEventNotifier;
        this.timeProvider = timeProvider;
    }

    @Override
    public ChannelReadStateResult updateChannelReadState(long accountId, long channelId, long lastReadMessageId) {
        return transactionRunner.runInTransaction(afterCommit -> {
            CPChannelMessage message = messageRepository.findById(lastReadMessageId)
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            if (message.channelId() != channelId) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            channelContextApi.requireMemberContext(channelId, accountId);
            ChannelReadState current = readStateRepository
                    .findByChannelIdAndAccountId(channelId, accountId)
                    .orElse(null);
            // 消息 ID 仅定位锚点；晚分配的雪花 ID 不代表晚提交，已读边界按频道序号推进。
            if (current != null && current.lastReadSequence() >= message.sequence()) {
                return toResult(current);
            }
            Instant now = this.timeProvider.nowInstant();
            ChannelReadState updated = new ChannelReadState(
                    channelId,
                    accountId,
                    lastReadMessageId,
                    message.sequence(),
                    current == null ? now: current.createdAt(),
                    now
            );
            if (readStateRepository.advanceIfNewer(updated)) {
                messageEventNotifier.readStateUpdated(afterCommit, updated);
                return toResult(updated);
            }
            return readStateRepository.findByChannelIdAndAccountId(channelId, accountId)
                    .map(this::toResult)
                    .orElseGet(() -> toResult(updated));
        });
    }

    @Override
    public List<ChannelUnreadResult> listUnreads(long accountId) {
        return readStateRepository.listUnreadsByAccountId(accountId).stream()
                .map(item -> new ChannelUnreadResult(
                        item.channelId(),
                        item.unreadCount()
                ))
                .toList();
    }

    private ChannelReadStateResult toResult(ChannelReadState readState) {
        return new ChannelReadStateResult(
                readState.channelId(),
                readState.accountId(),
                readState.lastReadMessageId()
        );
    }
}

package team.carrypigeon.backend.chat.domain.features.message.domain.service.readstate;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import team.carrypigeon.backend.chat.domain.features.channel.domain.api.context.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.readstate.result.ChannelReadSummaryResult;
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
    public ChannelReadSummaryResult getChannelReadState(long accountId, long channelId) {
        return transactionRunner.runInTransaction(afterCommit -> {
            channelContextApi.requireMemberContext(channelId, accountId);
            Long anchor = readStateRepository.findByChannelIdAndAccountId(channelId, accountId)
                    .map(ChannelReadState::lastReadMessageId).orElse(null);
            // 使用响应中的同一个锚点计数，首次访问不会创建已读记录。
            long unread = readStateRepository.countUnreads(channelId, accountId, anchor == null ? 0L : anchor);
            return new ChannelReadSummaryResult(channelId, accountId, anchor, unread);
        });
    }

    @Override
    public ChannelReadStateResult updateChannelReadState(long accountId, long channelId, long lastReadMessageId) {
        return transactionRunner.runInTransaction(afterCommit -> {
            // 已读也是关联写入，必须在读取锚点前取得频道锁，防止清理后重新写入状态。
            channelContextApi.requireMemberContextForUpdate(channelId, accountId);
            CPChannelMessage message = messageRepository.findById(lastReadMessageId)
                    .orElseThrow(() -> ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND));
            if (message.channelId() != channelId) {
                throw ProblemException.of(MessageProblem.MESSAGE_NOT_FOUND);
            }
            ChannelReadState current = readStateRepository
                    .findForUpdate(channelId, accountId)
                    .orElse(null);
            // 多端重复或较早的上报返回已保存的锚点。
            if (current != null && current.lastReadMessageId() >= lastReadMessageId) {
                return toResult(current);
            }
            Instant now = this.timeProvider.nowInstant();
            ChannelReadState updated = new ChannelReadState(
                    channelId,
                    accountId,
                    lastReadMessageId,
                    current == null ? now : current.createdAt(),
                    now
            );
            readStateRepository.save(updated);
            messageEventNotifier.readStateUpdated(afterCommit, updated);
            return toResult(updated);
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

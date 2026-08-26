package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.chat.domain.features.channel.domain.api.ChannelContextApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessageReadStateApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.UpdateChannelReadStateCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelReadStateResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.projection.ChannelUnreadResult;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.ChannelReadStateRepository;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.id.Ids;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner;

/**
 * 消息读状态领域 API 实现。
 * 职责：校验最后已读消息与频道成员关系，并维护只前进不后退的读状态。
 * 边界：未读统计由读状态仓储提供，不读取 channel 内部模型或仓储。
 */
@Service
public class MessageReadStateDomainApi implements MessageReadStateApi {

    private final MessageRepository messageRepository;
    private final ChannelReadStateRepository readStateRepository;
    private final ChannelContextApi channelContextApi;
    private final TransactionRunner transactionRunner;
    private final MessageAfterCommitPublisher afterCommitPublisher;

    public MessageReadStateDomainApi(
            MessageRepository messageRepository,
            ChannelReadStateRepository readStateRepository,
            ChannelContextApi channelContextApi,
            TransactionRunner transactionRunner,
            ApplicationEventPublisher eventPublisher,
            TimeProvider timeProvider
    ) {
        this.messageRepository = messageRepository;
        this.readStateRepository = readStateRepository;
        this.channelContextApi = channelContextApi;
        this.transactionRunner = transactionRunner;
        this.afterCommitPublisher = new MessageAfterCommitPublisher(eventPublisher, timeProvider);
    }

    @Override
    public ChannelReadStateResult updateChannelReadState(UpdateChannelReadStateCommand command) {
        validate(command);
        return transactionRunner.runInTransaction(afterCommit -> {
            ChannelMessage message = messageRepository.findById(command.lastReadMessageId())
                    .orElseThrow(() -> ProblemException.notFound("message does not exist"));
            if (message.channelId() != command.channelId()) {
                throw ProblemException.notFound("message does not exist");
            }
            channelContextApi.requireMemberChannel(command.channelId(), command.accountId());
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
            readStateRepository.upsert(updated);
            afterCommitPublisher.publishReadStateUpdatedAfterCommit(afterCommit, updated);
            return toResult(updated);
        });
    }

    @Override
    public List<ChannelUnreadResult> listUnreads(long accountId) {
        requirePositive(accountId, "accountId");
        return readStateRepository.listUnreadsByAccountId(accountId).stream()
                .map(item -> new ChannelUnreadResult(
                        Ids.toString(item.channelId()),
                        item.unreadCount(),
                        item.lastReadTime() == null ? 0L : item.lastReadTime().toEpochMilli()
                ))
                .toList();
    }

    private void validate(UpdateChannelReadStateCommand command) {
        if (command == null) {
            throw ProblemException.validationFailed("command must not be null");
        }
        requirePositive(command.accountId(), "accountId");
        requirePositive(command.channelId(), "channelId");
        requirePositive(command.lastReadMessageId(), "lastReadMessageId");
        requirePositive(command.lastReadTime(), "lastReadTime");
    }

    private void requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw ProblemException.validationFailed(fieldName + " must be greater than 0");
        }
    }

    private ChannelReadStateResult toResult(ChannelReadState readState) {
        return new ChannelReadStateResult(
                Ids.toString(readState.channelId()),
                Ids.toString(readState.accountId()),
                Ids.toString(readState.lastReadMessageId()),
                readState.lastReadTime().toEpochMilli()
        );
    }
}

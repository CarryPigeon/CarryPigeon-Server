package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import team.carrypigeon.backend.chat.domain.features.channel.domain.projection.ChannelPinReference;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessagePinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageUnpinnedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.ReadStateUpdatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息事务后实时发布协作对象。
 * 职责：从 canonical 消息构造创建、撤回、置顶和提醒事件。
 * 边界：不读取仓储，不重建 domain data，不发布编辑或硬删除事件。
 */
class MessageAfterCommitPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    MessageAfterCommitPublisher(ApplicationEventPublisher eventPublisher, TimeProvider timeProvider) {
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new ReadStateUpdatedEvent(
                readState.channelId(), readState.accountId(), readState.lastReadMessageId(), readState.lastReadTime()
        )));
    }

    void publishMessageCreatedAfterCommit(
            AfterCommitExecutor afterCommit,
            ChannelMessage message,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
        afterCommit.execute(() -> {
            publishMessageCreated(message, recipientAccountIds);
            publishMentions(mentions);
        });
    }

    void publishMessageRecalledAfterCommit(
            AfterCommitExecutor afterCommit,
            ChannelMessage message,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new MessageRecalledEvent(
                message.channelId(),
                message.messageId(),
                timeProvider.nowMillis(),
                recipientAccountIds
        )));
    }

    void publishMessagePinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            ChannelPinReference pin,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new MessagePinnedEvent(
                pin.channelId(),
                pin.messageId(),
                pin.pinId(),
                pin.pinnedByAccountId(),
                pin.pinnedAt(),
                recipientAccountIds
        )));
    }

    void publishMessageUnpinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            ChannelPinReference pin,
            long unpinnedByAccountId,
            long unpinnedAt,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> eventPublisher.publishEvent(new MessageUnpinnedEvent(
                pin.channelId(),
                pin.messageId(),
                pin.pinId(),
                unpinnedByAccountId,
                unpinnedAt,
                recipientAccountIds
        )));
    }

    private void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            eventPublisher.publishEvent(new MentionCreatedEvent(
                    mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(),
                    mention.targetAccountId(), mention.createdAt()
            ));
        }
    }

    private void publishMessageCreated(ChannelMessage message, List<Long> recipientAccountIds) {
        eventPublisher.publishEvent(new MessageCreatedEvent(
                message.messageId(), message.senderId(), message.channelId(), message.domain(), message.domainVersion(),
                message.data(), message.sendTime(), message.mentions(), message.preview(),
                message.status().name().toLowerCase(Locale.ROOT), recipientAccountIds
        ));
    }
}

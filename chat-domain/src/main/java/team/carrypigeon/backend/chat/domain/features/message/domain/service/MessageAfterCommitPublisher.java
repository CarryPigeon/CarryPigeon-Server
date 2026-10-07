package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Locale;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MentionCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageCreatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.MessageRecalledEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.event.ReadStateUpdatedEvent;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.infrastructure.basic.event.AfterCommitEventPublisher;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProviderImpl;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息事务后实时发布协作对象。
 * 职责：从 canonical 消息构造创建、撤回和提醒事件。
 * 边界：不读取仓储，不重建 domain data，不发布编辑或硬删除事件。
 */
class MessageAfterCommitPublisher {

    private final AfterCommitEventPublisher eventPublisher;
    private final TimeProviderImpl timeProvider;

    MessageAfterCommitPublisher(AfterCommitEventPublisher eventPublisher, TimeProviderImpl timeProvider) {
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    void publishReadStateUpdatedAfterCommit(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        eventPublisher.publishAfterCommit(
                afterCommit::execute,
                new ReadStateUpdatedEvent(
                        readState.channelId(), readState.accountId(), readState.lastReadMessageId(), readState.lastReadTime()
                )
        );
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
        eventPublisher.publishAfterCommit(
                afterCommit::execute,
                new MessageRecalledEvent(
                        message.channelId(),
                        message.messageId(),
                        timeProvider.nowMillis(),
                        recipientAccountIds
                )
        );
    }

    private void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            eventPublisher.publishAfterCommit(
                    eventPublisher::publish,
                    new MentionCreatedEvent(
                            mention.mentionId(), mention.channelId(), mention.messageId(), mention.fromAccountId(),
                            mention.targetAccountId(), mention.createdAt()
                    )
            );
        }
    }

    private void publishMessageCreated(ChannelMessage message, List<Long> recipientAccountIds) {
        eventPublisher.publishAfterCommit(
                eventPublisher::publish,
                new MessageCreatedEvent(
                        message.messageId(), message.senderId(), message.channelId(), message.domain(), message.domainVersion(),
                        message.data(), message.sendTime(), message.mentions(), message.preview(),
                        message.status().name().toLowerCase(Locale.ROOT), recipientAccountIds
                )
        );
    }
}

package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.MessageRealtimePublisher;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息事务后实时发布协作对象。
 * 职责：集中登记消息、置顶和 mention 相关的 after-commit 发布动作。
 * 边界：只负责提交后的实时发布，不读取仓储、不修改消息状态。
 */
class MessageAfterCommitPublisher {

    private final MessageRealtimePublisher messageRealtimePublisher;

    MessageAfterCommitPublisher(MessageRealtimePublisher messageRealtimePublisher) {
        this.messageRealtimePublisher = messageRealtimePublisher;
    }

    void publishMessageCreatedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PersistedMessage persistedMessage
    ) {
        afterCommit.execute(() -> {
            messageRealtimePublisher.publish(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    void publishMessageUpdatedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PersistedMessage persistedMessage
    ) {
        afterCommit.execute(() -> {
            messageRealtimePublisher.publishUpdate(
                    persistedMessage.message(),
                    persistedMessage.senderSnapshot(),
                    persistedMessage.recipientAccountIds()
            );
            publishMentions(persistedMessage.mentions());
        });
    }

    void publishMessagePinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PinnedChannelMessage pinnedChannelMessage
    ) {
        afterCommit.execute(() -> messageRealtimePublisher.publishPin(
                pinnedChannelMessage.pin(),
                pinnedChannelMessage.recipientAccountIds()
        ));
    }

    void publishMessageUnpinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.UnpinnedChannelMessage unpinnedChannelMessage
    ) {
        afterCommit.execute(() -> messageRealtimePublisher.publishUnpin(
                unpinnedChannelMessage.pin(),
                unpinnedChannelMessage.unpinnedByAccountId(),
                unpinnedChannelMessage.unpinnedAt(),
                unpinnedChannelMessage.recipientAccountIds()
        ));
    }

    private void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            messageRealtimePublisher.publishMentionCreated(mention, List.of(mention.targetAccountId()));
        }
    }
}

package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.command.PublishRealtimeEventCommand;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.service.database.api.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息事务后实时发布协作对象。
 * 职责：从 canonical 消息构造创建、撤回、置顶和提醒事件。
 * 边界：不读取仓储，不重建 domain data，不发布编辑或硬删除事件。
 */
class MessageAfterCommitPublisher {

    private final RealtimeEventApi realtimeEventApi;
    private final TimeProvider timeProvider;

    MessageAfterCommitPublisher(RealtimeEventApi realtimeEventApi, TimeProvider timeProvider) {
        this.realtimeEventApi = realtimeEventApi;
        this.timeProvider = timeProvider;
    }

    void publishMessageCreatedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PersistedMessage persistedMessage
    ) {
        afterCommit.execute(() -> {
            publishMessageCreated(persistedMessage);
            publishMentions(persistedMessage.mentions());
        });
    }

    void publishMessageRecalledAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PersistedMessage persistedMessage
    ) {
        afterCommit.execute(() -> publish(
                persistedMessage.message().channelId(),
                "message.recalled",
                Map.of(
                        "cid", Long.toString(persistedMessage.message().channelId()),
                        "mid", Long.toString(persistedMessage.message().messageId()),
                        "recall_time", timeProvider.nowMillis()
                ),
                persistedMessage.recipientAccountIds()
        ));
    }

    void publishMessagePinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.PinnedChannelMessage pinnedChannelMessage
    ) {
        afterCommit.execute(() -> publish(
                pinnedChannelMessage.pin().channelId(),
                "message.pinned",
                Map.of(
                        "cid", Long.toString(pinnedChannelMessage.pin().channelId()),
                        "mid", Long.toString(pinnedChannelMessage.pin().messageId()),
                        "pin_id", Long.toString(pinnedChannelMessage.pin().pinId()),
                        "pinned_by_uid", Long.toString(pinnedChannelMessage.pin().pinnedByAccountId()),
                        "pinned_at", pinnedChannelMessage.pin().pinnedAt().toEpochMilli()
                ),
                pinnedChannelMessage.recipientAccountIds()
        ));
    }

    void publishMessageUnpinnedAfterCommit(
            AfterCommitExecutor afterCommit,
            AbstractMessageDomainSupport.UnpinnedChannelMessage unpinnedChannelMessage
    ) {
        afterCommit.execute(() -> publish(
                unpinnedChannelMessage.pin().channelId(),
                "message.unpinned",
                Map.of(
                        "cid", Long.toString(unpinnedChannelMessage.pin().channelId()),
                        "mid", Long.toString(unpinnedChannelMessage.pin().messageId()),
                        "pin_id", Long.toString(unpinnedChannelMessage.pin().pinId()),
                        "unpinned_by_uid", Long.toString(unpinnedChannelMessage.unpinnedByAccountId()),
                        "unpinned_at", unpinnedChannelMessage.unpinnedAt()
                ),
                unpinnedChannelMessage.recipientAccountIds()
        ));
    }

    private void publishMentions(List<Mention> mentions) {
        for (Mention mention : mentions) {
            publish(mention.channelId(), "mention.created", Map.of(
                    "mention_id", Long.toString(mention.mentionId()),
                    "cid", Long.toString(mention.channelId()),
                    "mid", Long.toString(mention.messageId()),
                    "from_uid", Long.toString(mention.fromAccountId()),
                    "uid", Long.toString(mention.targetAccountId()),
                    "created_at", mention.createdAt().toEpochMilli()
            ), List.of(mention.targetAccountId()));
        }
    }

    private void publishMessageCreated(AbstractMessageDomainSupport.PersistedMessage persistedMessage) {
        ChannelMessage message = persistedMessage.message();
        publish(message.channelId(), "message.created", Map.of(
                "cid", Long.toString(message.channelId()),
                "message", canonicalMessage(message)
        ), persistedMessage.recipientAccountIds());
    }

    private Map<String, Object> canonicalMessage(ChannelMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mid", Long.toString(message.messageId()));
        payload.put("uid", Long.toString(message.senderId()));
        payload.put("cid", Long.toString(message.channelId()));
        payload.put("domain", message.domain());
        payload.put("domain_version", message.domainVersion());
        payload.put("data", message.data());
        payload.put("send_time", message.sendTime().toEpochMilli());
        payload.put("mentions", message.mentions().stream().map(String::valueOf).toList());
        payload.put("preview", message.preview());
        payload.put("status", message.status().name().toLowerCase(Locale.ROOT));
        return payload;
    }

    private void publish(long channelId, String eventType, Object payload, List<Long> recipients) {
        realtimeEventApi.publish(new PublishRealtimeEventCommand(channelId, eventType, payload, recipients, true));
    }
}

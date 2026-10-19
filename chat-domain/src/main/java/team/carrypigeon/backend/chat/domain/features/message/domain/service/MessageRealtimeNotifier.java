package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.MessageBody;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.MessageCreatedPayload;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.MessageRealtimeEventTypes;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.MessageRecalledPayload;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.MentionCreatedPayload;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.event.ReadStateUpdatedPayload;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.RealtimeEventApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.event.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 消息实时通知协作对象。
 * 职责：把消息、提及与已读事实转换为实时事件，并登记到事务提交之后执行。
 * 边界：不读取仓储、不重建 domain data；不发布编辑或硬删除事件。
 */
@Component
public class MessageRealtimeNotifier {

    private final RealtimeEventApi realtimeEventApi;
    private final TimeProvider timeProvider;

    public MessageRealtimeNotifier(RealtimeEventApi realtimeEventApi, TimeProvider timeProvider) {
        this.realtimeEventApi = realtimeEventApi;
        this.timeProvider = timeProvider;
    }

    /**
     * 通知已读位置更新，仅发给本人。
     *
     * @param afterCommit 事务提交后执行器
     * @param readState 已读状态快照
     */
    public void readStateUpdated(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                MessageRealtimeEventTypes.READ_STATE_UPDATED,
                new ReadStateUpdatedPayload(
                        Long.toString(readState.channelId()),
                        Long.toString(readState.accountId()),
                        Long.toString(readState.lastReadMessageId()),
                        readState.lastReadTime().toEpochMilli()
                ),
                List.of(readState.accountId())
        )));
    }

    /**
     * 通知新消息，并按被提及账号逐个下发提及事件。
     *
     * @param afterCommit 事务提交后执行器
     * @param message 已持久化的消息快照
     * @param recipientAccountIds 频道成员账号快照
     * @param mentions 本次消息产生的提及
     */
    public void messageCreated(
            AfterCommitExecutor afterCommit,
            ChannelMessage message,
            List<Long> recipientAccountIds,
            List<Mention> mentions
    ) {
        afterCommit.execute(() -> {
            realtimeEventApi.publish(new RealtimeEvent(
                    MessageRealtimeEventTypes.MESSAGE_CREATED,
                    new MessageCreatedPayload(Long.toString(message.channelId()), toBody(message)),
                    recipientAccountIds
            ));
            for (Mention mention : mentions) {
                realtimeEventApi.publish(new RealtimeEvent(
                        MessageRealtimeEventTypes.MENTION_CREATED,
                        new MentionCreatedPayload(
                                Long.toString(mention.mentionId()),
                                Long.toString(mention.channelId()),
                                Long.toString(mention.messageId()),
                                Long.toString(mention.fromAccountId()),
                                Long.toString(mention.targetAccountId()),
                                mention.createdAt().toEpochMilli()
                        ),
                        List.of(mention.targetAccountId())
                ));
            }
        });
    }

    /**
     * 通知消息撤回。
     *
     * @param afterCommit 事务提交后执行器
     * @param message 撤回后的消息快照
     * @param recipientAccountIds 频道成员账号快照
     */
    public void messageRecalled(
            AfterCommitExecutor afterCommit,
            ChannelMessage message,
            List<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                MessageRealtimeEventTypes.MESSAGE_RECALLED,
                new MessageRecalledPayload(
                        Long.toString(message.channelId()),
                        Long.toString(message.messageId()),
                        timeProvider.nowMillis()
                ),
                recipientAccountIds
        )));
    }

    private MessageBody toBody(ChannelMessage message) {
        return new MessageBody(
                Long.toString(message.messageId()),
                Long.toString(message.senderId()),
                Long.toString(message.channelId()),
                message.domain(),
                message.domainVersion(),
                message.data(),
                message.sendTime().toEpochMilli(),
                message.mentions().stream().map(String::valueOf).toList(),
                message.preview(),
                message.status().name().toLowerCase(Locale.ROOT)
        );
    }
}

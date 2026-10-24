package team.carrypigeon.backend.chat.domain.features.message.adaptor.realtime;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import team.carrypigeon.backend.chat.domain.features.message.domain.capability.MessageEventNotifier;
import team.carrypigeon.backend.infrastructure.basic.plugin.message.CPChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelReadState;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEvent;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventApi;
import team.carrypigeon.backend.infrastructure.basic.realtime.downlink.RealtimeEventType;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;
import team.carrypigeon.backend.infrastructure.basic.transaction.TransactionRunner.AfterCommitExecutor;

/**
 * 基于实时通道的消息事件通知适配器。
 * 职责：把域内的消息事件意图翻译为线上事件名与载荷，并在事务提交后交给实时通道发布。
 * 边界：线上事件名、字段类型与取值约正则全部收敛在本类与其载荷记录中；不读取仓储、不重建 domain data。
 */
@Component
public class RealtimeMessageEventNotifier implements MessageEventNotifier {

    private static final RealtimeEventType MESSAGE_CREATED = new RealtimeEventType("message.created");
    private static final RealtimeEventType MESSAGE_RECALLED = new RealtimeEventType("message.recalled");
    private static final RealtimeEventType READ_STATE_UPDATED = new RealtimeEventType("read_state.updated");

    private final RealtimeEventApi realtimeEventApi;
    private final TimeProvider timeProvider;

    public RealtimeMessageEventNotifier(RealtimeEventApi realtimeEventApi, TimeProvider timeProvider) {
        this.realtimeEventApi = realtimeEventApi;
        this.timeProvider = timeProvider;
    }

    @Override
    public void readStateUpdated(AfterCommitExecutor afterCommit, ChannelReadState readState) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                READ_STATE_UPDATED,
                new ReadStateUpdatedPayload(
                        Long.toString(readState.channelId()),
                        Long.toString(readState.accountId()),
                        Long.toString(readState.lastReadMessageId()),
                        readState.lastReadTime().toEpochMilli()
                ),
                List.of(readState.accountId())
        )));
    }

    @Override
    public void messageCreated(
            AfterCommitExecutor afterCommit,
            CPChannelMessage message,
            Collection<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> {
            realtimeEventApi.publish(new RealtimeEvent(
                    MESSAGE_CREATED,
                    new MessageCreatedPayload(Long.toString(message.channelId()), toBody(message)),
                    List.copyOf(recipientAccountIds)
            ));
        });
    }

    @Override
    public void messageRecalled(
            AfterCommitExecutor afterCommit,
            CPChannelMessage message,
            Collection<Long> recipientAccountIds
    ) {
        afterCommit.execute(() -> realtimeEventApi.publish(new RealtimeEvent(
                MESSAGE_RECALLED,
                new MessageRecalledPayload(
                        Long.toString(message.channelId()),
                        Long.toString(message.messageId()),
                        timeProvider.nowMillis()
                ),
                List.copyOf(recipientAccountIds)
        )));
    }

    /**
     * 把领域消息快照翻译为线上消息体。
     *
     * @param message 消息快照
     * @return 线上消息体
     */
    private MessageBody toBody(CPChannelMessage message) {
        return new MessageBody(
                Long.toString(message.messageId()),
                Long.toString(message.senderId()),
                Long.toString(message.channelId()),
                message.domain(),
                message.data(),
                message.sendTime().toEpochMilli(),
                message.mentions().stream().map(String::valueOf).toList(),
                message.preview(),
                message.status().name().toLowerCase(Locale.ROOT)
        );
    }
}
